/**
 * JAVA SOCKET CLIENT
 * 
 * @author Aastha Bhatt
 * @author Matthew Dibbern
 * @author Vincent Gruse
 * @author Leo Tangban
 * @author Emmanuel Taylor
 * 
 * @description
 *    This class represents a Java UDP Client that sends a request to a server containing a web server
 *    and timer value. The client receives data from server and sends exactly one ACK to the server.
 * 
 * @packages
 *    Java IO (BufferedReader, ByteArrayOutputStream, IOException, InputStreamReader)
 *    Java Net (DatagramPacket, DatagramSocket, InetAddress)
 *    Java NIO Charset (StandardCharsets)
 */

 import java.io.BufferedReader;
 import java.io.ByteArrayOutputStream;
 import java.io.IOException;
 import java.io.InputStreamReader;
 import java.net.DatagramPacket;
 import java.net.DatagramSocket;
 import java.net.InetAddress;
 import java.nio.charset.StandardCharsets;

public class Client {
    private final static int PORT = 11111;

    public static void main(String[] args) {
        DatagramSocket clientSocket = null;

        System.out.println("\n==================================================================================");
        System.out.println("=========================JAVA SOCKET APPLICATION - CLIENT=========================");
        System.out.println("==================================================================================\n");

        try {
            clientSocket = new DatagramSocket();
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            // Collect user input.
            System.out.print("Enter the web server name: ");
            String webServerName = userInput.readLine();
            System.out.print("Enter timer value in seconds: ");
            int timerValue = Integer.parseInt(userInput.readLine());

            // Start timer.
            long startTime = System.currentTimeMillis();

            // Send the client request to the server.
            String clientRequest = webServerName + "," + timerValue;
            byte[] sendData = clientRequest.getBytes();
            InetAddress serverAddress = InetAddress.getLocalHost();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, PORT);
            clientSocket.send(sendPacket);

            // Retrieve data packets from the server.
            byte[] receiveData = new byte[1024];
            ByteArrayOutputStream receivedDataBuffer = new ByteArrayOutputStream();

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length, serverAddress, PORT);
                clientSocket.receive(receivePacket);
                String receiveMessage = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
                
                String[] parts = receiveMessage.split("xxx");
                receivedDataBuffer.write(parts[1].getBytes(), 0, parts[1].length());

                String[] headers = parts[0].split(",");
                if (Integer.parseInt(headers[0]) == Integer.parseInt(headers[1]) - 1) {
                    break;
                }
            }
            
            // Close ClientSocket.
            clientSocket.close();
            
            // Stop timer.
            long elapsedTime = System.currentTimeMillis() - startTime;

            // If the packets were recieved within the time limit, print the contents,
            // and send an ACK to the server. Otherwise, print FAIL.
            if (elapsedTime < timerValue * 1000) {
                DatagramSocket ackSocket = new DatagramSocket();
                String ackMessage = "ACK";
                byte[] ackData = ackMessage.getBytes();
                DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, serverAddress, PORT + 1);
                ackSocket.send(ackPacket);
                String recievedData = receivedDataBuffer.toString();
                System.out.println("\n" + recievedData + "\n");
                System.out.println("OK");
                ackSocket.close();
            } else {
                System.out.println("\nFAIL");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close(); // Close the serverSocket in the finally block
            }
        }
    }
}
