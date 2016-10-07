import com.microsoft.msr.malmo.*;

import static java.lang.System.loadLibrary;

/**
 * A Java translation of the Python "tutorial_1" example for the Malmo platform made by Microsoft
 */
public class Tutorial1 {

    static {
        loadLibrary("MalmoJava"); // Load the Malmo JNI
    }

    public static void main(String[] argv) {
        AgentHost agentHost = new AgentHost();
        try {
            StringVector args = new StringVector();
            args.add("Tutorial1");
            for(String arg: argv)
                args.add(arg);
        } catch(Exception e) {
            System.out.format("ERROR: %s", e.getMessage());
            System.out.println(agentHost.getUsage());
        }
        if(agentHost.receivedArgument("help")) {
            System.out.println(agentHost.getUsage());
            System.exit(0);
        }

        MissionSpec missionSpec = new MissionSpec();                                        // Initialize our mission
        MissionRecordSpec missionRecordSpec = new MissionRecordSpec("./saved_data.tgz");    // Initialize where we want to store our mission logs

        // Try to start the mission
        int maxRetries = 3;                 // How many times will we try to start our mission
        for(int i=0; i<maxRetries; i++) {
            try {
                // Start the mission - hope it doesn't throw an exception
                agentHost.startMission(missionSpec, missionRecordSpec);
                break;
            } catch(Exception e) {
                // Failed to start the mission since an exception was thrown.
                if(i == maxRetries - 1) {   // Failed to start the mission as many times as we defined in "maxRetries"
                    System.out.format("ERROR: %s", e.getMessage());
                    System.out.format("Couldn't start the mission after trying %d times. Exiting mission.", maxRetries);
                    System.exit(1);
                }

                System.out.format("Failed to start the mission after %d/%d attempts. Retrying in 2 seconds.", i+1, maxRetries);
                try {
                    Thread.sleep(2000);
                } catch(InterruptedException e2) {
                    System.out.format("ERROR: %s", e2.getMessage());
                    System.out.println("Couldn't sleep the thread for two seconds");
                }
            }
        }

        // Wait for the mission to start
        WorldState worldState = agentHost.getWorldState();
        while(!worldState.getHasMissionBegun()) {   // Wait for the mission to begin
            System.out.print(".");
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
                System.out.format("ERROR: %s", e.getMessage());
                System.out.println("Couldn't sleep the thread for 0.1 second while waiting for the mission to begin");
            }

            worldState = agentHost.getWorldState();
            for(int i=0; i<worldState.getErrors().size(); i++) {
                System.out.format("ERROR: "+worldState.getErrors().get(i));
            }
        }

        // Log that the mission started
        System.out.println("\nMission started");

        // Do something. In our case, we do nothing for 10 seconds.
        while(worldState.getIsMissionRunning()) {
            System.out.print(".");

            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
                System.out.format("ERROR: %s", e.getMessage());
                System.out.println("Couldn't sleep the thread for 0.1 second while the mission's executing");
            }

            worldState = agentHost.getWorldState();
            for(int i=0; i<worldState.getErrors().size(); i++) {
                System.out.format("ERROR: "+worldState.getErrors().get(i));
            }
        }

        System.out.println("\n\nMission has ended.");
    }

}
