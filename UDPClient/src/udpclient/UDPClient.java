/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package udpclient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.net.SocketTimeoutException;
import java.util.Random;

/**
 *
 * @author hh299
 */
public class UDPClient {
    
    //the default port of amin server and default ip
    private  final String TFTP_SERVER_IP = "127.0.0.1";
    private  final int TFTP_DEFAULT_PORT = 1025;
    
    //generate a random port for a client
    private Random r = new Random();
    private int portClient = r.nextInt(65535 - 1024 + 1) + 1024;
    
    
    
    // TFTP OP Codes
    private  final byte OP_RRQ = 1;
    private final byte OP_WRQ = 2;
    private final byte OP_DATAPACKET = 3;
    private final byte OP_ACK = 4;
    private final byte OP_ERROR = 5;
    
    /**
     * create a datagramSocket 
     * and empty instance of inetAddress to later store the address of a received packet from the new server thread 
    */
    private DatagramSocket datagramSocket = null;
    private InetAddress inetAddress = null;
    private byte[] bufferByteArray;
    
    /**
     * create DatagramPackets for storing in the the address
     */
    private DatagramPacket outBoundDatagramPacket;
    private DatagramPacket inBoundDatagramPacket;
    private int blockNum = 1; 
    
    

    /**
     * run the client through argument and parse those argument to 
     * a client simulator 
     */
    public static void main(String[] args) throws IOException
    {
        UDPClient c = new UDPClient();
        c.clientSimulator(args);
    }
    
    /**
     * main client simulator derives all the arguments and handles writing or 
     * reading requests to main server
     */
    private void clientSimulator(String[] args) throws IOException 
    {
        //check the arguments
        byte op; 
        
        if (args[0].equals("read"))
        {
            op = OP_RRQ;
        }
        else if (args[0].equals("write"))
        {
            op = OP_WRQ;
        }
        else{return;}
        
        //creat the default octet and get the filename
        String mode = "octet";
        String fileName = args[1];
        
        //convert the octet and filename into bytes
        byte[] modeByte = mode.getBytes();
        byte[] fileNameByte = fileName.getBytes();
        
        //create an array of bites and fiill up with data for the request
        ByteArrayOutputStream byteOutOS = new ByteArrayOutputStream();
        byteOutOS.write(0);
        byteOutOS.write(op);
        byteOutOS.write(fileNameByte);    
        byteOutOS.write(0);    
        byteOutOS.write(modeByte);    
        byteOutOS.write(0);
        
        //crweate a packet and fill up with the bites
        byte[] packet = byteOutOS.toByteArray();
        
        //give the adress of a socket
        inetAddress = InetAddress.getByName(TFTP_SERVER_IP);
	datagramSocket = new DatagramSocket(portClient);
        
        //set a timer
        datagramSocket.setSoTimeout(5000);
        
        // prepare for communication to send
        outBoundDatagramPacket = new DatagramPacket(packet,
        packet.length, inetAddress, TFTP_DEFAULT_PORT);
        
        // send the request packet
        datagramSocket.send(outBoundDatagramPacket);
        
        
        //create buffer array to store in
        bufferByteArray = new byte[516];
        
        
        // prepare for communication to recieve
        inBoundDatagramPacket = new DatagramPacket(bufferByteArray,
	bufferByteArray.length, inetAddress,
        datagramSocket.getLocalPort());
        
        
        //try to recieve
        try
        {
            datagramSocket.receive(inBoundDatagramPacket);
        }catch(SocketException e)
        {}
        
        //DECLARE AN ARRAY FOR RECIEVING A PACKET FROM A SERVER
        ByteArrayOutputStream byteInOS = new ByteArrayOutputStream();
        
        //for later use in the client
        boolean done = false;
        
        /**
         * start checking if the received packet
         * involves the  Ack/dataPacket/Error 
         * then once it finishes dealing with server it 
         * closes the socket in the end 
         */
        if(bufferByteArray[1] == OP_ACK)
        {
            
            //CHECK FOR FILE AND RUN EXCEPTION IF NOT FOUND ENDING THE METHOD 
            byte[] bFile; 
            try {
                Path path = Paths.get(fileName);
                bFile = Files.readAllBytes(path);
            } catch (FileNotFoundException e) 
            {
                return;
            }
            
            //DECLARE VARIABELS FOR THE LATER USE
            ByteArrayOutputStream packetFill = new ByteArrayOutputStream();
            
            //set teh vlock araray to be consistent of blocknum int
            byte[] blockNumArray = new byte[2];
            
            //blockNumArray = new byte[2];
            blockNumArray[1]=(byte)blockNum;
            
            //declare variabels to check the size and the number of times sent 
            int currentSize;
            int offset = 0;
            
           //check if the lenght of the file exceeds the 512 
           //if it is then change the current size
            if(bFile.length < 512*(offset+1)){
                currentSize = bFile.length - (512*offset);
                done = true;
            }else{
                currentSize = 512;
            }
            
            
            //fill up the packet first packet with the blocknumber 1
            packetFill.write(0);
            packetFill.write(OP_DATAPACKET);
            packetFill.write(blockNumArray);
            packetFill.write(bFile, offset*512, currentSize);
            offset++;
            
            //create the first packet and fill it up with the array of bytes
            byte[] packet1 = packetFill.toByteArray();
            
            //prepare connaction to send
            outBoundDatagramPacket = new DatagramPacket(packet1,
                            packet1.length, inBoundDatagramPacket.getAddress(), inBoundDatagramPacket.getPort());
            
            
            // try to send
            try 
            {
                datagramSocket.send(outBoundDatagramPacket);
                
            } catch (SocketException e) {}
            
            //close the socket if lower than 512
            if(done)
            {
                datagramSocket.close();
                return;
            }
            
            
            
            //DO-WDOHILE LOOP TO KEEP SENDING WHILE NEXT PACKET IS OF SMALLER SIZE
            do
            {
                //try to recieve the next ackif not then timout exception resend the data 
                try
                {
                    datagramSocket.receive(inBoundDatagramPacket);
                }catch (SocketTimeoutException e)
                {
                    datagramSocket.send(outBoundDatagramPacket);
                }
                
                // check the boolean variabels in case resendin is needed
                if( currentSize  < 1)
                {
                    return;
                }
                
                //increment the blocknum
                blockNum++;
                
                //check if the block numbers are equal 
                if(bufferByteArray[3] == blockNumArray[1])
                {
                    // set to null to fill up and send again
                    packetFill.reset();
                    
                    //change the blocknum
                    blockNumArray = new byte[2];
                    blockNumArray[1]=(byte)blockNum;
                    
                    // fill up the array
                    packetFill.write(0);
                    packetFill.write(OP_DATAPACKET);
                    packetFill.write(blockNumArray);

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
                            incPacket.length, inetAddress, inBoundDatagramPacket.getPort());
                    try
                    {
                        datagramSocket.send(outBoundDatagramPacket);
                    }catch (SocketException e){}
                    
                }
                
            }while(currentSize == 512);//(outBoundDatagramPacket.getLength() == 516));
            
        }else if(bufferByteArray[1] == OP_DATAPACKET)
        {
            ByteArrayOutputStream ackFill = new ByteArrayOutputStream();
            ByteArrayOutputStream recieveed = new ByteArrayOutputStream();
            
            
            byte[] blockNumArray = new byte[2];
            blockNumArray[1]=(byte)blockNum;
            
            ackFill.write(0);
            ackFill.write(OP_ACK);
            
            try {
                ackFill.write(blockNumArray);
            } catch (IOException ex) {
                //Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            
            //fill up the packet
            byte[] ackPack = ackFill.toByteArray();
            
            //prepare the connection 
            outBoundDatagramPacket = new DatagramPacket(ackPack,
                        ackPack.length, inBoundDatagramPacket.getAddress(), inBoundDatagramPacket.getPort());
            
            // send the first ack
            try 
            {
                
                datagramSocket.send(outBoundDatagramPacket);
                
            } catch (SocketException e) {} catch (IOException ex) {}
            
            //recieve an array
            recieveed.write(bufferByteArray, 4, inBoundDatagramPacket.getLength()-4);
                    
            try
            {
                do 
                {
                    //increment the block num for every packet
                    blockNum++;
                    
                    //force the in varible block num to be in bytes of 2
                    blockNumArray = new byte[2];
                    blockNumArray[1]=(byte)blockNum;
                    
                    //recieve the next packet 
                    datagramSocket.receive(inBoundDatagramPacket);
                                 
                    if(bufferByteArray[3] == blockNumArray[1]) 
                    {
                        recieveed.write(bufferByteArray, 4, inBoundDatagramPacket.getLength()-4);
                        try 
                        {
                            // set to 0 before you send
                            ackFill.reset();
                            ackFill.write(0);
                            ackFill.write(OP_ACK);
                            ackFill.write(blockNumArray);
                            
                            //create an packet
                            byte[] ackPacket = ackFill.toByteArray();
                            
                            //make connection
                            outBoundDatagramPacket = new DatagramPacket(ackPacket,
                                    ackPacket.length, inBoundDatagramPacket.getAddress(), inBoundDatagramPacket.getPort());
                            
                            // send the packet
                            datagramSocket.send(outBoundDatagramPacket);
                            
                            //if(inBoundDatagramPacket.getLength() < 516){break;}
                        } catch (SocketException e) {}
                    }
                } while (inBoundDatagramPacket.getLength() == 516); 
                
                //store the recieved bytes into local memoty into a file 
                OutputStream outputStream = new FileOutputStream(fileName);
                recieveed.writeTo(outputStream);
                
            }catch(SocketException e){} catch (IOException ex) {}
            
        }else if(bufferByteArray[1] == OP_ERROR)
        {
            datagramSocket.close();
        }
        
        datagramSocket.close();
    }
}
