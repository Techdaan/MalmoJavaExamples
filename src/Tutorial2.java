import com.microsoft.msr.malmo.*;

import java.util.StringJoiner;

/**
 * A Java translation of the Python "tutorial_1" example for the Malmo platform made by Microsoft
 *
 * This tutorial will make the actor do nothing but stand still for thirty seconds.
 * Notice how all the code is practically the same. We only provided an XML file.
 */
public class Tutorial2 {

    static {
        System.loadLibrary("MalmoJava"); // Load the Malmo JNI
    }

    public static void main(String[] argv) {
        String missionXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
                "            <Mission xmlns=\"http://ProjectMalmo.microsoft.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "            \n" +
                "              <About>\n" +
                "                <Summary>Hello world!</Summary>\n" +
                "              </About>\n" +
                "              \n" +
                "              <ServerSection>\n" +
                "                <ServerHandlers>\n" +
                "                  <FlatWorldGenerator generatorString=\"3;7,220*1,5*3,2;3;,biome_1\"/>\n" +
                "                  <ServerQuitFromTimeUp timeLimitMs=\"30000\"/>\n" +
                "                  <ServerQuitWhenAnyAgentFinishes/>\n" +
                "                </ServerHandlers>\n" +
                "              </ServerSection>\n" +
                "              \n" +
                "              <AgentSection mode=\"Survival\">\n" +
                "                <Name>MalmoTutorialBot</Name>\n" +
                "                <AgentStart/>\n" +
                "                <AgentHandlers>\n" +
                "                  <ObservationFromFullStats/>\n" +
                "                  <ContinuousMovementCommands turnSpeedDegs=\"180\"/>\n" +
                "                </AgentHandlers>\n" +
                "              </AgentSection>\n" +
                "            </Mission>";


        AgentHost agent = new AgentHost();
        try {
            StringVector args = new StringVector();
            args.add("Tutorial2");
            for(String arg: argv)
                args.add(arg);
        } catch(Exception e) {
            System.out.format("ERROR: %s", e.getMessage());
            System.out.println(agent.getUsage());
        }
        if(agent.receivedArgument("help")) {
            System.out.println(agent.getUsage());
            System.exit(0);
        }

        MissionSpec mission = null;
        MissionRecordSpec missionRecord = null;
        try {
            mission = new MissionSpec(missionXML, true);
            missionRecord = new MissionRecordSpec("./saved_data.tgz");
        } catch(Exception e) {
            System.out.format("ERROR: %s", e.getMessage());
            System.out.println("Occured when trying to initialize the new MissionSpec. Exiting mission");
            System.exit(1);
        }

        // Try to start the mission
        int maxRetries = 3;                 // How many times will we try to start our mission
        for(int i=0; i<maxRetries; i++) {
            try {
                // Start the mission - hope it doesn't throw an exception
                agent.startMission(mission, missionRecord);
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
        WorldState worldState = agent.getWorldState();
        while(!worldState.getHasMissionBegun()) {   // Wait for the mission to begin
            System.out.print(".");
            try {
                Thread.sleep(100);
            } catch(InterruptedException e) {
                System.out.format("ERROR: %s", e.getMessage());
                System.out.println("Couldn't sleep the thread for 0.1 second while waiting for the mission to begin");
            }

            worldState = agent.getWorldState();
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

            worldState = agent.getWorldState();
            for(int i=0; i<worldState.getErrors().size(); i++) {
                System.out.format("ERROR: "+worldState.getErrors().get(i));
            }
        }

        System.out.println("\n\nMission has ended.");
    }
}
