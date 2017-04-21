import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

public class ProxyServer extends Thread
{
    static String cacheFolder = "server";
    static String indexFilePath = cacheFolder + File.separator + "cacheIndex.txt";
    static String logFilePath = "log.txt";
    ServerSocket serverSocket = null;
    Socket proxySocket = null;
    String websiteUrl;
    BufferedReader clientReader = null;
    PrintWriter cachedWebPageWriter = null;
    PrintStream clientStream = null;
    BufferedReader websiteResponseReader = null;
    BufferedReader cachedResponseReader = null;
    String indexLine;
    String statusLogCode;

    public ProxyServer(Socket proxySocket)
    {
        this.proxySocket = proxySocket;
        start();
    }

    public void run()
    {
        try
        {
            getClientRequest();
            if(getURLAndCheckIsBlackListed() == true)
            {
                statusLogCode = "BLOCK";
                updateLog(statusLogCode);
                clientStream.println("-2");
                return;
            }
            if(checkIsWebsiteCached() == true)
            {
                serveCachedPage();
                statusLogCode = "CACHE";
                updateLog(statusLogCode);
            }
            else
            {
                try
                {
                    getWebServerResponseAndServeClient();
                }
                catch(IOException e)
                {
                    clientStream.println("-1");
                    throw e;
                }
            }
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
        finally
        {
            freeResources();
        }
    }

    private void getClientRequest() throws IOException
    {
        clientReader = new BufferedReader(new InputStreamReader(proxySocket.getInputStream()));
        clientStream = new PrintStream(proxySocket.getOutputStream());
        System.out.println("Ready to read data");
    }

    private boolean getURLAndCheckIsBlackListed() throws IOException
    {
        boolean isBlackListed = false;
        websiteUrl = clientReader.readLine();
        Scanner blackListScanner = new Scanner(new File("blacklist.txt"));
        while(blackListScanner.hasNextLine())
        {
            String blackListedLine = blackListScanner.nextLine();
            if((blackListedLine.indexOf(websiteUrl)) != -1)
            {
                int firstIndex = blackListedLine.indexOf(" ");
                int secondIndex = blackListedLine.lastIndexOf(" ");
                String startTime = blackListedLine.substring(firstIndex + 1, secondIndex);
                String endTime = blackListedLine.substring(secondIndex + 1);
                Date todayDate = Calendar.getInstance().getTime();
                Date startDate = evaluateStartAndEndTime(startTime);
                Date endDate = evaluateStartAndEndTime(endTime);

                if(todayDate.after(startDate) && todayDate.before(endDate))
                {
                    isBlackListed = true;
                    break;
                }
            }
        }
        blackListScanner.close();
        return isBlackListed;
    }

    private Date evaluateStartAndEndTime(String time)
    {
        int YYYY = Integer.parseInt(time.substring(0, 4));
        int MM = Integer.parseInt(time.substring(4, 6));
        int DD = Integer.parseInt(time.substring(6, 8));
        int hh = Integer.parseInt(time.substring(8, 10));
        int mm = Integer.parseInt(time.substring(10, 12));
        int ss = Integer.parseInt(time.substring(12, 14));
        Calendar mydate = Calendar.getInstance();
        mydate.set(YYYY, MM, DD, hh, mm, ss);
        return mydate.getTime();
    }

    private boolean checkIsWebsiteCached() throws IOException
    {
        boolean isCached = false;
        File f = new File(indexFilePath);
        Scanner scanner = new Scanner(f);
        System.out.println("URL received: "+ websiteUrl);
        while(scanner.hasNextLine())
        {
            indexLine = scanner.nextLine();
            if((indexLine.indexOf(websiteUrl)) != -1)
            {
                isCached = true;
                break;
            }
        }
        scanner.close();
        return isCached;
    }

    private void serveCachedPage() throws IOException
    {
        int index = indexLine.indexOf(" ");
        String cachedFileName = indexLine.substring(index+1);
        System.out.println("Serve cached page: "+cachedFileName);
        String currentLine;
        cachedResponseReader = new BufferedReader(new FileReader(cacheFolder + File.separator + cachedFileName));
        while ((currentLine = cachedResponseReader.readLine()) != null)
        {
            clientStream.println(currentLine);
            clientStream.flush();
        }
        cachedResponseReader.close();
    }

    private void getWebServerResponseAndServeClient() throws IOException, MalformedURLException
    {
        int statusCode = 0;

        if((websiteUrl.indexOf("https://") == -1) && (websiteUrl.indexOf("http://") == -1))
        {
            websiteUrl = "http://"+ websiteUrl;
        }

        URL webServer = new URL(websiteUrl);
        URLConnection webUrlConnection = webServer.openConnection();
        HttpURLConnection httpConnection = null;
        if(webUrlConnection instanceof HttpURLConnection)
        {
            httpConnection = (HttpURLConnection) webUrlConnection;
            statusCode = httpConnection.getResponseCode();
            System.out.println("statusCode: "+statusCode);
        }

        if(statusCode == 302)
        {
            statusLogCode = "BADRQ";
            updateLog(statusLogCode);
            clientStream.println("-4");
            return;
        }

        String response = scanResponse(httpConnection);
        if(response == "")
        {
            statusLogCode = "INAPP";
            updateLog(statusLogCode);
            clientStream.println("-3");
            return;
        }

        String cachedFileName = getFileName();
        String filePath = cacheFolder + File.separator + cachedFileName;
        cachedWebPageWriter = new PrintWriter(filePath);
        System.out.println("Getting response from the webserver");
        if(response != null)
        {
            if(statusCode == 200)
            {
                cachedWebPageWriter.println(response);
                cachedWebPageWriter.flush();
            }
            clientStream.println(response);
            clientStream.flush();
        }
        statusLogCode = "OK200";
        updateLog(statusLogCode);
        deleteIndexFileEntryIfRequired();
        updateIndexFile(statusCode, cachedFileName);
    }

    private String scanResponse(HttpURLConnection httpConnection) throws IOException
    {
        boolean isInAppropriate = false;
        BufferedReader in = new BufferedReader(new InputStreamReader(httpConnection.getInputStream()));
        String inputLine;
        String response = null;
        while((inputLine = in.readLine()) != null)
        {
            Scanner scanner = new Scanner(new File("lang.txt"));
            while(scanner.hasNextLine())
            {
                String line = scanner.nextLine();
                if((inputLine.indexOf(line)) != -1)
                {
                    response = "";
                    isInAppropriate = true;
                    break;
                }
            }
            scanner.close();
            if(isInAppropriate == true)
            {
                break;
            }
            response = response + inputLine;
        }
        in.close();
        return response;
    }

    private synchronized void deleteIndexFileEntryIfRequired() throws IOException
    {
        int lineCount = 0;
        Scanner input = new Scanner(new File(indexFilePath));
        while (input.hasNextLine())
        {
            String data = input.nextLine();
            lineCount++;
        }
        if(lineCount == 5)
        {
            File path = new File(indexFilePath);
            Scanner scanner = new Scanner(path);
            ArrayList<String> coll = new ArrayList<String>();
            String deletingLine = scanner.nextLine();
            while (scanner.hasNextLine())
            {
                String line = scanner.nextLine();
                coll.add(line);
            }
            scanner.close();
            PrintWriter pw = new PrintWriter(indexFilePath);
            pw.close();
            FileWriter writer = new FileWriter(path);
            for (String line : coll)
            {
                writer.write(line);
                writer.write("\n");
            }
            writer.close();
            String cachedFileToBeDeleted = deletingLine.substring(deletingLine.indexOf(" ") + 1);
            File f = new File(cacheFolder + File.separator + cachedFileToBeDeleted);
            f.delete();
        }
    }

    private synchronized void updateIndexFile(int statusCode, String cachedFileName) throws IOException
    {
        if(statusCode == 200)
        {
            File myFile = new File (indexFilePath);
            String dataToIndexFile = websiteUrl + " " +  cachedFileName;
            FileWriter mappingIndexFileWriter = new FileWriter (myFile, true);
            mappingIndexFileWriter.append(dataToIndexFile);
            mappingIndexFileWriter.append("\n");
            mappingIndexFileWriter.flush();
            mappingIndexFileWriter.close();
        }
    }

    private synchronized void updateLog(String statusLogCode) throws IOException
    {
        File myFile = new File (logFilePath);
        DateFormat dateFormat = new SimpleDateFormat("YYYYMMddHHmmss");
        Calendar cal = Calendar.getInstance();
        String dataToLogFile = dateFormat.format(cal.getTime()) + " " + websiteUrl + " " +  statusLogCode;
        FileWriter logFileWriter = new FileWriter (myFile, true);
        logFileWriter.append(dataToLogFile);
        logFileWriter.append("\n");
        logFileWriter.flush();
        logFileWriter.close();
    }

    private String getFileName()
    {
        DateFormat dateFormat = new SimpleDateFormat("YYYYMMddHHmmss");
        Calendar cal = Calendar.getInstance();

        String fileName = websiteUrl.replace('/', '_');
        fileName = fileName + dateFormat.format(cal.getTime());
        return fileName;
    }

    private void freeResources()
    {
        try
        {
            if(websiteResponseReader != null)
            {
                websiteResponseReader.close();
            }
            if(cachedWebPageWriter != null)
            {
                cachedWebPageWriter.close();
            }
            if(clientStream != null)
            {
                clientStream.close();
            }
            if(clientReader != null)
            {
                clientReader.close();
            }
            if(proxySocket != null)
            {
                proxySocket.close();
            }
        }
        catch(IOException e)
        {
            System.out.println("Error in closing " + e);
        }
    }

    public static void main(String[] args)
    {
        if(args.length != 1)
        {
            System.out.println("Proper Usage is: java ProxyServer <portNumber>");
            return;
        }

        ServerSocket serverSocket = null;
        try
        {
            int portNumber = Integer.parseInt(args[0]);
            serverSocket = new ServerSocket(portNumber);

            // Create the cache index file, if it doesn't exists
            File cacheIndexFile = new File(indexFilePath);
            cacheIndexFile.getParentFile().mkdirs();
            cacheIndexFile.createNewFile();
        }
        catch (IOException e)
        {
            System.out.println(e);
            return;
        }

        try
        {
            while(true)
            {
                System.out.println("Waiting for the client request..");
                Socket proxySocket = serverSocket.accept();
                System.out.println("Connection established");
                ProxyServer serverObj = new ProxyServer(proxySocket);
            }
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
    }
}
