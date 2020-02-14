package tcpserver;

import com.sun.corba.se.spi.activation.Server;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class TCPServer 
{
    
    // default ip and the port of tftp main server
    private  final String TFTP_SERVER_IP = "127.0.0.1";
    private  final int TFTP_DEFAULT_PORT = 1025;
    
    // create instance of a socket and inetaddress to assign the recieved packets ip 
    private DatagramSocket datagramSocket;
    private InetAddress inetAddress = null;
    
    // creaate a instanc eof a datagramPacket to store recieved packet
    private DatagramPacket inBoundDatagramPacket;
    
    //array of byte store the derived bytes from recieved packet
    private byte[] bufferByteArray;
    
    // call the main server method in here
    public static void main(String[] args) throws SocketException {
        TCPServer s = new TCPServer() {};
    }
    
    private TCPServer() throws SocketException
    {
        // create a socket
        datagramSocket = new DatagramSocket(TFTP_DEFAULT_PORT);
         
        // try - create a new server thread parse the recieved packet to new server 
        try {
            bufferByteArray = new byte[516];
            while (true) 
            {
                inBoundDatagramPacket = new DatagramPacket(bufferByteArray,
                        bufferByteArray.length, inetAddress,
                        datagramSocket.getLocalPort());
                
                datagramSocket.receive(inBoundDatagramPacket);
                
                (new Thread((Runnable) new TCPServerThread(inBoundDatagramPacket))).start();
            }
        } catch (SocketException e) 
        {
            
        }catch (IOException e) 
        {
            
        }
    }
}
