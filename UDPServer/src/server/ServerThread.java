/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

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

/**
 *
 * @author hh299
 */
public class ServerThread  implements Runnable {
    
    //op codes in bytes
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
    private int portServer = r.nextInt(65535 - 1024 + 1) + 1024;
    
    public static void main(String[] args) 
    {
        // TODO code application logic here
    }
    
    DatagramPacket packet = null;
    
    /**
     * parse the packet from a main server to here derive 
     * the packet with the constructor 
     */
    public ServerThread(DatagramPacket packet) 
    {
        this.packet = packet;
    }

    @Override
    public void run(){
        // DECLARE A VARIABLE IN BYTE FOR LATER TO ASSIGN 
        byte[] bFile = null;
        
        // DECLARE THE MESSAGE YOU WANT TO SEND IF AN ERROR ACCURS
        String errorMessage = "ERROR FILE NOT FOUND";
        
        //declare  an aray to fill the filename
        ByteArrayOutputStream fileName = new ByteArrayOutputStream();
        
        
        //AN ERROR ARRAY
        byte[] errorByte = errorMessage.getBytes();
        
        //buffer array to keep th packet in
        bufferByteArray = new byte[516];
        bufferByteArray = packet.getData();
        
        //try to make a socket 
        try {
            datagramSocket = new DatagramSocket(portServer);
        } catch (SocketException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            //make a socket for a server
            inetAddress = InetAddress.getByName(TFTP_SERVER_IP);
        } catch (UnknownHostException ex) {
            Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //recive the packet and parse it in here
        inBoundDatagramPacket = new DatagramPacket(bufferByteArray,
	bufferByteArray.length, packet.getAddress(),
        packet.getPort());
        
        //iterate int that get incremented once the bufferbyte array reaches the 0  byte
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
            try
            {
                
                //get the file name check the path and read them in bytes
                try
                {
                    Path path = Paths.get(name);
                    bFile = Files.readAllBytes(path);
                    
                }catch(FileNotFoundException e)
                {} catch (NoSuchFileException ex) 
                {
                    byte[] errorNum = new byte[2];
                    ByteArrayOutputStream errorArray = new ByteArrayOutputStream();
                    errorArray.write(0);
                    errorArray.write(OP_ERROR);
                    errorArray.write(errorNum);
                    errorArray.write(errorByte);
                    errorArray.write(0);
                    /////////////////////////////////////////////////////////////////////fill up the packet
                    byte[] packetError = errorArray.toByteArray();
                    //////////////////////////////////////////////////////////////////////connection
                    outBoundDatagramPacket = new DatagramPacket(packetError,
                        packetError.length, packet.getAddress(), packet.getPort());
                    ////////////////////////////////////////////////////////////////////////
                    datagramSocket.send(outBoundDatagramPacket);
                }
                
                //declare the array for filling up the packet
                ByteArrayOutputStream packetFill = new ByteArrayOutputStream();
                
                //force the block num into byte
                byte[] blockNumArray = new byte[2];
                blockNumArray[1] = (byte)blockNum;
                
                //set the current size int with the offset of the bytes
                int currentSize;
                int offset = 0;
                
                if(bFile.length < 512*(offset+1))
                {
                    currentSize = bFile.length - (512*offset);
                    done = true;
                }else
                {
                    currentSize = 512;
                }
                
                
                //send the first packet with the blocknumber 1
                packetFill.write(0);
                packetFill.write(OP_DATAPACKET);
                packetFill.write(blockNumArray);
                
                packetFill.write(bFile, offset*512, currentSize);
                offset++;
                
                //creat the first packet and will it up with the array of bytes
                byte[] packet1 = packetFill.toByteArray();
                
                //prepare connaction to send
                outBoundDatagramPacket = new DatagramPacket(packet1,
                        packet1.length, packet.getAddress(), packet.getPort());
                
                
                try
                {
                    datagramSocket.send(outBoundDatagramPacket);
                    
                }catch(SocketException e){}
                
                if(done)
                {
                    datagramSocket.close();
                    return;
                }
                
                //do while loop
                do
                {
                    //try to recieve  and resend if not recieved
                    try
                    {
                        datagramSocket.receive(inBoundDatagramPacket);
                        
                    }catch (SocketTimeoutException e)
                    {
                        datagramSocket.send(outBoundDatagramPacket);
                    }
                    
                    
                    if( currentSize  < 1)
                    {
                        return;
                    }
                    
                    //increment the blocknum
                    blockNum++;
                    
                    if (bufferByteArray[3] == blockNumArray[1])
                    {
                        
                        // set to zero for fillin it up again
                        packetFill.reset();

                        //change the blocknum
                        blockNumArray = new byte[2];
                        blockNumArray[1] = (byte) blockNum;

                        // fill up the array
                        packetFill.write(0);
                        packetFill.write(OP_DATAPACKET);
                        packetFill.write(blockNumArray);

                        if (bFile.length < (512 * (offset + 1))) {
                            currentSize = bFile.length - (512 * offset);
                        } else {
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
                        } catch (SocketException e) 
                        {
                        }

                    }

                } while (currentSize == 512);
                
                
            }catch(IOException ex)
            {   Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
}
            
            
        }else if(bufferByteArray[1] == OP_WRQ)
        {
            //array to fill up the acks
            ByteArrayOutputStream ackFill = new ByteArrayOutputStream();
            
            //set is as  0 because the first ack contain a block num of 0
            blockNum = 0;
            
            byte[] blockNumArray = new byte[2];
            blockNumArray[1]=(byte)blockNum;
            
            ackFill.write(0);
            ackFill.write(OP_ACK);
            
            try 
            {
                ackFill.write(blockNumArray);
            } catch (IOException ex) 
            {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
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
            } catch (SocketException e) {} catch (IOException ex) 
            {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            
            //recieve array
            ByteArrayOutputStream recieveed = new ByteArrayOutputStream();
            
            try
            {
                
                do 
                {
                    //increent the block num
                    blockNum++;
                    
                    //force the block num to become a byte of 2
                    blockNumArray = new byte[2];
                    blockNumArray[1]=(byte)blockNum;
                    
                    //recieve
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
                                    ackPacket.length, inetAddress, packet.getPort());
                            
                            // send the packet
                            datagramSocket.send(outBoundDatagramPacket);
                            
                        } catch (SocketException e) {}
                    }
                } while (inBoundDatagramPacket.getLength() == 516);
                
                // store the recieved bytes into local memoty into a file 
                OutputStream outputStream = new FileOutputStream(name);
                recieveed.writeTo(outputStream);
                
            }catch(SocketException e){} catch (IOException ex) {
                Logger.getLogger(ServerThread.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
  }

