import java.io.*;
import java.net.*;
import java.util.*;

public class Client
{
    Socket clientSocket = null;
    String websiteUrl = null;
    String fileName = null;
    PrintStream pos = null;
    BufferedReader reader = null;
    PrintWriter writer = null;

    private int getAndValidateUrl()
    {
        Scanner in = new Scanner(System.in);
        System.out.print("Enter the URL: ");
        websiteUrl = in.nextLine();
        if(websiteUrl.isEmpty())
        {
            return -5;
        }
        in.close();
        return 0;
    }

    private void setWebpageFileName()
    {
        fileName = websiteUrl.replace('/', '_');
        if(fileName.indexOf(".htm") == -1)
        {
            fileName = fileName + ".htm";
        }
    }

    private void establishConnection(String hostName, int portNumber) throws IOException, UnknownHostException
    {
        clientSocket = new Socket(hostName, portNumber);
    }

    private void sendRequestToServer() throws IOException, UnknownHostException
    {
        pos = new PrintStream(clientSocket.getOutputStream());
        pos.println(websiteUrl);
    }

    private int getAndWriteResponse() throws IOException, UnknownHostException
    {
        String responseLine;
        reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        responseLine = reader.readLine();
        if(responseLine.equals("-1"))
        {
            return -1;
        }
        else if(responseLine.equals("-2"))
        {
            return -2;
        }
        else if(responseLine.equals("-3"))
        {
            return -3;
        }
        else if(responseLine.equals("-4"))
        {
            return -4;
        }
        writer = new PrintWriter(fileName, "UTF-8");
        writer.println(responseLine);
        writer.flush();
        while ((responseLine = reader.readLine()) != null)
        {
            writer.println(responseLine);
            writer.flush();
        }
        return 0;
    }

    private void freeResources()
    {
        try
        {
            if(reader != null)
            {
                reader.close();
            }
            if(pos != null)
            {
                pos.close();
            }
            if(writer != null)
            {
                writer.close();
            }
            if(clientSocket != null)
            {
                clientSocket.close();
            }
        }
        catch(IOException e)
        {
            System.out.println("Error in closing " + e);
        }
    }

    public static void main(String[] args)
    {
        if(args.length != 2)
        {
            System.out.println("Proper Usage is: java Client <server hostname/IP Address> <portNumber>");
            System.exit(0);
        }

        Client client = new Client();
        int portNumber = Integer.parseInt(args[1]);
        String hostName = args[0];
        try
        {
            if(client.getAndValidateUrl() == -5)
            {
                System.out.println("Error Message: URL can't be null");
                return;
            }
            client.setWebpageFileName();
            client.establishConnection(hostName, portNumber);
        }
        catch (UnknownHostException e)
        {
            System.err.println("Unknown host: " + hostName);
        }
        catch (IOException e)
        {
            System.err.println("Couldn't connect to: " + hostName);
        }

        if (client.clientSocket != null)
        {
            try
            {
                client.sendRequestToServer();
                int errorCode = client.getAndWriteResponse();
                if(errorCode == -1)
                {
                    System.out.println("Error Message: Invalid Website URL");
                }
                else if(errorCode == -2)
                {
                    System.out.println("Error Message: Website is blocked");
                }
                else if(errorCode == -3)
                {
                    System.out.println("Error Message: Webpage contains inappropriate language");
                }
                else if(errorCode == -4)
                {
                    System.out.println("Error Message: Bad Request (Incorrect Protocol)");
                }
            }
            catch(UnknownHostException e)
            {
                System.err.println("Trying to connect to unknown host: " + e);
            }
            catch(IOException e)
            {
                System.err.println("IOException:  " + e);
            }
            finally
            {
                client.freeResources();
            }
        }
    }
}
