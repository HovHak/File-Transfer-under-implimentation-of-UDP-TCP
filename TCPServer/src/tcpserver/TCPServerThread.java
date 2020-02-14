
package tcpserver;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;


public class TCPServerThread implements Runnable 
{
    //the op codes in bytes
    private  final byte OP_RRQ = 1;
    private final byte OP_WRQ = 2;
    private final byte OP_DATAPACKET = 3;
    private final byte OP_ACK = 4;
    private final byte OP_ERROR = 5;
    
    private  final String TFTP_SERVER_IP = "127.0.0.1";
    
    private DatagramSocket datagramSocket = null;
    private InetAddress inetAddress = null;
    
    private byte[] bufferByteArray;
    private DatagramPacket outBoundDatagramPacket;
    private DatagramPacket inBoundDatagramPacket;
    private int blockNum = 1; 
    
    private Random r = new Random();
    private int portServer = r.nextInt(67000 - 1024 + 1) + 1024;
    
    public static void main(String[] args) 
    {
        // TODO code application logic here
    }
    
    /**
     * get the parsed packet from the main server 
     */
    DatagramPacket packet = null;
    public TCPServerThread(DatagramPacket packet) 
    {
        this.packet = packet;
    }
    
    @Override
    public void run() 
    {
        // DECLARE A VARIABLE IN BYTE FOR LATER TO ASSIGN 
        byte[] bFile = null;
        
        // DECLARE THE MESSAGE YOU WANT TO SEND IF AN ERROR ACCURS
        String errorMessage = "ERROR FILE NOT FOUND";
        
        //
        ByteArrayOutputStream fileName = new ByteArrayOutputStream();
        
        
        //AN ERROR ARRAY
        byte[] errorByte = errorMessage.getBytes();
        
        //
        bufferByteArray = new byte[516];
        bufferByteArray = packet.getData();
        
        try {
            datagramSocket = new DatagramSocket(portServer);
        } catch (SocketException ex) {}
        try {
            //make a socket for a server
            inetAddress = InetAddress.getByName(TFTP_SERVER_IP);
        } catch (UnknownHostException ex) {
            Logger.getLogger(TCPServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        inBoundDatagramPacket = new DatagramPacket(bufferByteArray,
	bufferByteArray.length, packet.getAddress(),
        packet.getPort());
        
        
        int i = 2;
        while (bufferByteArray[i] != (byte) 0) 
        {
            fileName.write(bufferByteArray[i]);
            i++;
        }
        
        //force the byte to be converted to the name of the file 
        String name = fileName.toString();
        
        boolean done = false;
        
        if (bufferByteArray[1] == OP_RRQ)
        {
            
        
            //get the file name check the path and read them in bytes
            //send an error message if not found
            try 
            {
                Path path = Paths.get(name);
                bFile = Files.readAllBytes(path);

            } catch (FileNotFoundException e) {}
            catch (NoSuchFileException ex) 
            {
                byte[] errorNum = new byte[2];
                ByteArrayOutputStream errorArray = new ByteArrayOutputStream();
                errorArray.write(0);
                errorArray.write(OP_ERROR);
                try {
                    errorArray.write(errorNum);
                } catch (IOException ex1) {
                    Logger.getLogger(TCPServerThread.class.getName()).log(Level.SEVERE, null, ex1);
                }
                try {
                    errorArray.write(errorByte);
                } catch (IOException ex1) {
                    Logger.getLogger(TCPServerThread.class.getName()).log(Level.SEVERE, null, ex1);
                }
                errorArray.write(0);
                /////////////////////////////////////////////////////////////////////fill up the packet
                byte[] packetError = errorArray.toByteArray();
                //////////////////////////////////////////////////////////////////////connection
                outBoundDatagramPacket = new DatagramPacket(packetError,
                        packetError.length, packet.getAddress(), packet.getPort());
                try {
                    ////////////////////////////////////////////////////////////////////////
                    datagramSocket.send(outBoundDatagramPacket);
                } catch (IOException ex1) {}

                return;
            } catch (IOException ex) {}

            //declare the array for filling up the packet
            ByteArrayOutputStream packetFill = new ByteArrayOutputStream();

            //force the block num into byte
            byte[] blockNumArray = new byte[2];
            blockNumArray[1] = (byte) blockNum;

            int currentSize;
            int offset = 0;

            if (bFile.length < 512 * (offset + 1)) 
            {
                currentSize = bFile.length - (512 * offset);
                done = true;
            } else 
            {
                currentSize = 512;
            }
            
            //fill up the packet first packet with the blocknumber 1
            packetFill.write(0);
            packetFill.write(OP_DATAPACKET);
            try {
                packetFill.write(blockNumArray);
            } catch (IOException ex) {}
            
            packetFill.write(bFile, offset*512, currentSize);
            offset++;
            
            //creat the first packet and fill it up with the array of bytes
            byte[] packet1 = packetFill.toByteArray();
            
            //prepare connaction to send
            outBoundDatagramPacket = new DatagramPacket(packet1,
                            packet1.length, packet.getAddress(), packet.getPort());
            
            //send
            try 
            {
                datagramSocket.send(outBoundDatagramPacket);
                
            } catch (SocketException e) {} catch (IOException ex) {}
            
            //close the socket if lower than 512
            if(done)
            {
                datagramSocket.close();
                return;
            }
            
            //DO-WDOHILE LOOP TO KEEP SENDING WHILE NEXT PACKET IS OF SMALLER SIZE
            do 
            {
                if (currentSize < 1) 
                {
                    return;
                }

                //increment the blocknum
                blockNum++;

                // set to zero for fillin it up again
                packetFill.reset();

                //change the blocknum
                blockNumArray = new byte[2];
                blockNumArray[1] = (byte) blockNum;

                // fill up the array
                packetFill.write(0);
                packetFill.write(OP_DATAPACKET);
                try {
                    packetFill.write(blockNumArray);
                } catch (IOException ex) {}

                if (bFile.length < (512 * (offset + 1))) 
                {
                    currentSize = bFile.length - (512 * offset);
                } else 
                {
                    currentSize = 512;
                }

                packetFill.write(bFile, (offset * 512), currentSize);
                offset++;

                //store  them in a packet
                byte[] incPacket = packetFill.toByteArray();

                //give the adress and send
                outBoundDatagramPacket = new DatagramPacket(incPacket,
                        incPacket.length, packet.getAddress(), packet.getPort());/////////////////////////////////////////////////////////////////////////might need to change for the port
                try 
                {
                    datagramSocket.send(outBoundDatagramPacket);
                } catch (SocketException e) {} catch (IOException ex) {}
                
                
            }while (currentSize == 512);
            
        }else if(bufferByteArray[1] == OP_WRQ)
        {
            ByteArrayOutputStream ackFill = new ByteArrayOutputStream();
            
            blockNum = 0;
            
            byte[] blockNumArray = new byte[2];
            blockNumArray[1]=(byte)blockNum;
            
            ackFill.write(0);
            ackFill.write(OP_ACK);
            
            try {
                ackFill.write(blockNumArray);
            } catch (IOException ex) {
                Logger.getLogger(TCPServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            //fill up the packet
            byte[] ackPack = ackFill.toByteArray();
            
            //prepare the connection 
            outBoundDatagramPacket = new DatagramPacket(ackPack,
                        ackPack.length, packet.getAddress(), packet.getPort());
            
            // send the first ack
            try 
            {
                
                datagramSocket.send(outBoundDatagramPacket);
            } catch (SocketException e) {} catch (IOException ex) {
                Logger.getLogger(TCPServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //recieve array
            ByteArrayOutputStream recieveed = new ByteArrayOutputStream();
            
            do 
                {
                    //increent the block num
                    blockNum++;
                    
                    //forcethe block num to become a byte of 2
                    blockNumArray = new byte[2];
                    blockNumArray[1]=(byte)blockNum;
                    
                try {
                    //recieve
                    datagramSocket.receive(inBoundDatagramPacket);
                } catch (IOException ex) {
                    Logger.getLogger(TCPServerThread.class.getName()).log(Level.SEVERE, null, ex);
                }
                    
                    if(bufferByteArray[3] == blockNumArray[1]) 
                    {
                        recieveed.write(bufferByteArray, 4, inBoundDatagramPacket.getLength()-4);
                        
                    }
                } while (inBoundDatagramPacket.getLength() == 516);
            // store the recieved bytes into local memoty into a file 
            OutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(name);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(TCPServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                recieveed.writeTo(outputStream);
            } catch (IOException ex) {
                Logger.getLogger(TCPServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    
}
