/**
 * JAVA SOCKET SERVER
 * 
 * @author Aastha Bhatt
 * @author Matthew Dibbern
 * @author Vincent Gruse
 * @author Leo Tangban
 * @author Emmanuel Taylor
 * 
 * @description
 *    This class represents a Java UDP Socket Server that waits for request from a Client containing
 *    a web server and timer value. A new thread is created to handle client communication and processing.
 *    The server sends the response from the web server in packets of size 1000 and waits for an ACK from
 *    the client.
 * 
 * @packages
 *    Java IO (BufferedReader, ByteArrayOutputStream, DataOutputStream, IOException, InputStreamReader)
 *    Java Net (DatagramPacket, DatagramSocket, HttpURLConnection, InetAddress, SocketTimeoutException, URL)
 *    Java NIO Charset (StandardCharsets)
 *    Java Utilities (Arrays)
 */

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Server {
    private static final int PORT = 11111;

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(PORT)) {
            System.out.println("\n==================================================================================");
            System.out.println("=========================JAVA SOCKET APPLICATION - SERVER=========================");
            System.out.println("==================================================================================\n");

            System.out.println("Listening on Port " + PORT + "...\n");

            while (true) {
                byte[] receiveData = new byte[1024];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                String clientRequest = new String(receivePacket.getData(), 0, receivePacket.getLength());

                // Extract web server name and timer value from client request.
                String[] parts = clientRequest.split(",");
                String webServerName = parts[0];
                int timerValue = Integer.parseInt(parts[1]);
                System.out.println("Found Client Request: " + webServerName + ", " + timerValue + "\n");

                // Start a separate thread to handle client communication.
                Thread clientHandler = new Thread(new ClientHandler(webServerName, timerValue, receivePacket.getAddress(), receivePacket.getPort(), serverSocket));
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private static final int ACK_PORT = 11112;
    private static final int PACKET_SIZE = 1000;
    private static final int SOCKET_TIMEOUT = 3000;

    private final String webServerName;
    private final int timerValue;
    private final InetAddress clientAddress;
    private final int clientPort;
    private final DatagramSocket serverSocket;

    public ClientHandler(String webServerName, int timerValue, InetAddress clientAddress, int clientPort, DatagramSocket serverSocket) {
        this.webServerName = webServerName;
        this.timerValue = timerValue;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
        this.serverSocket = serverSocket;
    }

    @Override
    public void run() {
        try {
            sendDataToClient(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Name: sendDataToClient
     * 
     * Sends web server response data to the client and waits for an ACK from the client.
     * If the server fails to send an ACK within the time limit, it will retransmit once
     * more.
     * 
     * @param count (int)
     * @throws IOException
     */
    private void sendDataToClient(int count) throws IOException {
        if (count < 2) {
            try {
                long startTime = System.currentTimeMillis();
                String responseData = getResponseFromWebServer();
                sendResponseInPackets(responseData);
                waitForAck();
                long elapsedTime = System.currentTimeMillis() - startTime;
                System.out.println(elapsedTime < timerValue * 1000 ? "DONE\n" : "RESENT\n");
            } catch (SocketTimeoutException e) {
                if (count < 1) {
                    System.out.println("ACK not received from Client in time.\n\nRESENT\n");
                    count++;
                    sendDataToClient(count);
                } else {
                    System.out.println("ACK not received from Client after retransmission.\n");
                    count++;
                }
            }
        }
    }

    /**
     * Name: getResponseFromWebServer
     * 
     * Performs a GET request on the web server and returns the response data as a String.
     * 
     * @return (String)
     * @throws IOException
     */
    private String getResponseFromWebServer() throws IOException {
        URL url = new URL("https://" + webServerName);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder responseData = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                responseData.append(line);
            }
            return responseData.toString();
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Name: sendResponseInPackets
     * 
     * Divides responseData into chunks of PACKET_SIZE and sends each packet one at a time
     * to the client.
     * 
     * @param responseData (String)
     * @throws IOException
     */
    private void sendResponseInPackets(String responseData) throws IOException {
        byte[] responseDataBytes = responseData.getBytes(StandardCharsets.UTF_8);
        int totalPackets = (int) Math.ceil((double) responseDataBytes.length / PACKET_SIZE);

        for (int i = 0; i < totalPackets; i++) {
            int payloadLength = Math.min(PACKET_SIZE, responseDataBytes.length - i * PACKET_SIZE);
            byte[] payload = Arrays.copyOfRange(responseDataBytes, i * PACKET_SIZE, i * PACKET_SIZE + payloadLength);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (DataOutputStream dataOutputStream = new DataOutputStream(outputStream)) {
                dataOutputStream.writeBytes(i + "," + totalPackets + "," + payloadLength + "xxx");
                dataOutputStream.write(payload);
                byte[] packetData = outputStream.toByteArray();
                DatagramPacket packet = new DatagramPacket(packetData, packetData.length, clientAddress, clientPort);
                serverSocket.send(packet);
            }
        }
    }

    /**
     * Function Name: waitForAck
     * 
     * Once the packets have been send, the server will wait for an ACK from the client.
     * 
     * @throws IOException
     */
    private void waitForAck() throws IOException {
        try (DatagramSocket ackSocket = new DatagramSocket(ACK_PORT)) {
            ackSocket.setSoTimeout(SOCKET_TIMEOUT);
            byte[] receiveData = new byte[1024];
            DatagramPacket ackPacket = new DatagramPacket(receiveData, receiveData.length);
            ackSocket.receive(ackPacket);
            System.out.println("ACK received from Client.\n");
        }
    }
}