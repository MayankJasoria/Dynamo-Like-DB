package com.cloudproject.dynamo.msgmanager;

import java.io.IOException;
import java.util.Map;

import com.cloudproject.dynamo.vector_clock.core.MessageBufferPacker;
import com.cloudproject.dynamo.vector_clock.core.MessagePack;
import com.cloudproject.dynamo.vector_clock.core.MessageUnpacker;
import com.cloudproject.dynamo.vector_clock.vclock.VClock;

public class Vector {
	private DynamoNode dn;
	/*
    private final String pid;
    private VClock vc;*/
    public Vector(DynamoNode src) {
        this.dn=src;
    }

    /**
     * Returns the process id of the class.
     */
    /*public String getPid() {
        return pid;
    }

    /**
     * Returns the vector clock map contained in the class.
     */
    /*public VClock getVc() {
        return vc;
    }

    /**
     * Initialise the vector clock class and open a log file.
     */
    /*private void initVector() {

        this.vc = new VClock();
        this.vc.tick(this.pid);
        
    }
    */
  /*public synchronized byte[] prepareSend(String logMsg, byte packetContent) throws IOException {
        byte[] packetProxy = new byte[1];
        packetProxy[0] = packetContent;
        return prepareSend(logMsg, packetProxy);
    }*/

    private boolean updateClock() {
        long time = this.dn.getVc().findTicks(dn.name);
        if (time == -1) {
            System.err.println("Could not find process id in its vector clock.");
            return false;
        }
        this.dn.getVc().tick(dn.name);

        return true;
    }

    /**
     * Records a local event and increments the vector clock of this class.
     * Also appends a message in the log file defined in the vcInfo structure.
     *
     * @param logMsg Custom message will be written to the "vectorLog" log.
     */
    public synchronized void logLocalEvent(String logMsg) {
        updateClock();
    }

    /**
     * Encodes a buffer into a custom MessagePack byte array.
     * This is the default Vector method.
     * The function increments the vector clock, appends it to
     * the binary "packetContent" and converts the full message into MessagePack format
     */
    public synchronized byte[] prepareSend(byte[] packetContent) throws IOException {
        if (!updateClock()) return null;
        MessageBufferPacker packer = MessagePack.newDefaultBufferPacker();
        packer.packString(this.dn.name);
        packer.packBinaryHeader(packetContent.length);
        packer.writePayload(packetContent);
        packer.packMapHeader(this.dn.getVc().getClockMap().size()); // the number of (key, value) pairs
        for (Map.Entry<String, Long> clock : this.dn.getVc().getClockMap().entrySet()) {
            packer.packString(clock.getKey());
            packer.packLong(clock.getValue());
        }

        return packer.toByteArray();
    }

    /**
     * Encodes a buffer into a custom MessagePack byte array.
     * The function increments the vector clock contained of the Vectors, appends it to
     * the String "packetContent" and converts the full message into MessagePack format.
       */
    

    private void mergeRemoteClock(VClock remoteClock) {
        long time = this.dn.getVc().findTicks(this.dn.name);
        if (time == -1) {
            System.err.println("Could not find process id in its vector clock.");
            return;
        }
        this.dn.getVc().merge(remoteClock);
    }

    /**
     * Decodes a Vector buffer, updates the local vector clock, and returns the
     * decoded data.
     * This function takes a MessagePack buffer and extracts the vector clock as
     * well as data. It increments the local vector clock, merges the unpacked
     * clock with its own and returns a character representation of the data.
     */
    public synchronized byte[] unpackReceive(byte[] encodedMsg) throws IOException {
        long time = this.dn.getVc().findTicks(dn.name);
        if (time == -1) {
            System.err.println("Could not find process id in its vector clock.");
            return null;
        }

        // Deserialize with MessageUnpacker
        MessageUnpacker unpacker = MessagePack.newDefaultUnpacker(encodedMsg);
        String src_pid = unpacker.unpackString();
        int msglen = unpacker.unpackBinaryHeader();
        byte[] decodedMsg = unpacker.readPayload(msglen);
        int numClocks = unpacker.unpackMapHeader();
        VClock remoteClock = new VClock();
        for (int i = 0; i < numClocks; ++i) {
            String clock_pid = unpacker.unpackString();
            Long clock_time = unpacker.unpackLong();
            remoteClock.set(clock_pid, clock_time);
        }
        dn.getVc().tick(this.dn.name);
        mergeRemoteClock(remoteClock);
       
        unpacker.close();
        return decodedMsg;
    }

    
}
