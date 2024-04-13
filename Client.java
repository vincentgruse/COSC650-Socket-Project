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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class Client {
    private static final int SERVER_PORT = 11111;
    private static final int ACK_PORT = SERVER_PORT + 1;
    private static final int TIMEOUT = 5000; // Timeout in milliseconds

    public static void main(String[] args) {
        System.out.println("\n==================================================================================");
        System.out.println("=========================JAVA SOCKET APPLICATION - CLIENT=========================");
        System.out.println("==================================================================================\n");

        try (DatagramSocket clientSocket = new DatagramSocket()) {
            BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in));

            // Collect user input.
            System.out.print("Enter the web server name: ");
            String webServerName = userInput.readLine();
            System.out.print("Enter timer value in seconds: ");
            int timerValue = Integer.parseInt(userInput.readLine());

            // Send the client request to the server.
            InetAddress serverAddress = InetAddress.getLocalHost();
            sendRequest(clientSocket, webServerName, timerValue, serverAddress);

            // Receive and process data from the server.
            String receivedData = receiveData(clientSocket, serverAddress);

            // Print received data and send ACK if the operation was successful.
            if (receivedData != null) {
                System.out.println("\n" + receivedData + "\n");
                sendACK(clientSocket, serverAddress);
                System.out.println("OK");
            } else {
                System.out.println("\nFAIL");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void sendRequest(DatagramSocket socket, String webServerName, int timerValue, InetAddress serverAddress) throws IOException {
        String clientRequest = webServerName + "," + timerValue;
        byte[] sendData = clientRequest.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, SERVER_PORT);
        socket.send(sendPacket);
    }

    private static String receiveData(DatagramSocket socket, InetAddress serverAddress) throws IOException {
        byte[] receiveData = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        socket.setSoTimeout(TIMEOUT); // Set timeout for receiving data
        try {
            socket.receive(receivePacket);
            String receiveMessage = new String(receivePacket.getData(), 0, receivePacket.getLength(), StandardCharsets.UTF_8);
            String[] parts = receiveMessage.split("xxx");
            return parts[1];
        } catch (IOException e) {
            return null; // Timeout occurred
        }
    }

    private static void sendACK(DatagramSocket socket, InetAddress serverAddress) throws IOException {
        String ackMessage = "ACK";
        byte[] ackData = ackMessage.getBytes();
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, serverAddress, ACK_PORT);
        socket.send(ackPacket);
    }
}
