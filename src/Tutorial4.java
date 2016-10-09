import com.microsoft.msr.malmo.*;

/**
 * A Java translation of the Python "tutorial_4" example for the Malmo platform made by Microsoft
 *
 * In this tutorial you'll have to make the agent get to the diamond block located in the centre of the sponge.
 * A possible answer is in Tutorial4solved.java
 */
public class Tutorial4 {

    static {
        System.loadLibrary("MalmoJava");
    }

    private static String menger(int xorg, int yorg, int zorg, int size, String blocktype, String variant, String holetype) {
        // Draw a solid chunk
        String genString = genCuboidWithVariant(xorg, yorg, zorg, xorg+size-1, yorg+size-1, zorg+size-1, blocktype, variant) + "\n";

        // Remove holes
        int unit = size;
        while(unit>=3) {
            double w = unit/3;

            for(int i=0; i<size; i+=unit) {
                for(int j=0; j<size; j+=unit) {
                    int x, y, z;

                    x = xorg+i;
                    y = yorg+j;
                    genString += genCuboid((int) (x+w), (int) (y+w), zorg, (int) (x+2*w)-1, (int) (y+2*w)-1, zorg+size-1, holetype) + "\n";
                    y = yorg+i;
                    z = zorg+j;
                    genString += genCuboid(xorg, (int) (y+w), (int) (z+w), xorg+size-1, (int) (y+2*w)-1, (int) (z+2*w)-1, holetype) + "\n";
                    genString += genCuboid((int) (x+w), yorg, (int) (z+w), (int) (x+2*w)-1, yorg+size-1, (int) (z+2*w)-1, holetype) + "\n";
                }
            }
            unit /= 3;
        }

        return genString;
    }

    /**
     * Generate a solid cuboid
     * @param x1 X start pos
     * @param y1 Y start pos
     * @param z1 Z start pos
     * @param x2 X end pos
     * @param y2 Y end pos
     * @param z2 Z end pos
     * @param blocktype The block type to be used.
     * @return The string that should be used to generate the cuboid
     */
    private static String genCuboid(int x1, int y1, int z1, int x2, int y2, int z2, String blocktype) {
        return "<DrawCuboid x1=\"" + x1 + "\" y1=\"" + y1 + "\" z1=\"" + z1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" z2=\"" + z2 + "\" type=\"" + blocktype + "\" />";
    }

    /**
     * Generate a solid cuboid with a variant of the block type
     * @param x1 X start pos
     * @param y1 Y start pos
     * @param z1 Z start pos
     * @param x2 X end pos
     * @param y2 Y end pos
     * @param z2 Z end pos
     * @param blocktype The block type to be used.
     * @param variant The variant of the blocktype
     * @return The string that should be used to generate the cuboid
     */
    private static String genCuboidWithVariant(int x1, int y1, int z1, int x2, int y2, int z2, String blocktype, String variant) {
        return "<DrawCuboid x1=\"" + x1 + "\" y1=\"" + y1 + "\" z1=\"" + z1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" z2=\"" + z2 + "\" type=\"" + blocktype + "\" variant=\"" + variant + "\" />";
    }

    public static void main(String[] argv) {
        String missionXML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>\n" +
                "            <Mission xmlns=\"http://ProjectMalmo.microsoft.com\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "            \n" +
                "              <About>\n" +
                "                <Summary>Hello world!</Summary>\n" +
                "              </About>\n" +
                "              \n" +
                "            <ServerSection>\n" +
                "              <ServerInitialConditions>\n" +
                "                <Time>\n" +
                "                    <StartTime>1000</StartTime>\n" +
                "                    <AllowPassageOfTime>false</AllowPassageOfTime>\n" +
                "                </Time>\n" +
                "                <Weather>clear</Weather>\n" +
                "              </ServerInitialConditions>\n" +
                "              <ServerHandlers>\n" +
                "                  <FlatWorldGenerator generatorString=\"3;7,44*49,73,35:1,159:4,95:13,35:13,159:11,95:10,159:14,159:6,35:6,95:6;12;\"/>\n" +
                "                  <DrawingDecorator>\n" +
                "                    <DrawSphere x=\"-27\" y=\"70\" z=\"0\" radius=\"30\" type=\"air\"/>" + menger(-40, 40, -13, 27, "stone", "smooth_granite", "air") + "\n" +
                "                    <DrawBlock x=\"-27\" y=\"39\" z=\"0\" type=\"diamond_block\"/>\n" +
                "                  </DrawingDecorator>\n" +
                "                  <ServerQuitFromTimeUp timeLimitMs=\"30000\"/>\n" +
                "                  <ServerQuitWhenAnyAgentFinishes/>\n" +
                "                </ServerHandlers>\n" +
                "              </ServerSection>\n" +
                "              \n" +
                "              <AgentSection mode=\"Survival\">\n" +
                "                <Name>MalmoTutorialBot</Name>\n" +
                "                <AgentStart>\n" +
                "                    <Placement x=\"0.5\" y=\"56.0\" z=\"0.5\" yaw=\"90\"/>\n" +
                "                    <Inventory>\n" +
                "                        <InventoryItem slot=\"8\" type=\"diamond_pickaxe\"/>\n" +
                "                    </Inventory>\n" +
                "                </AgentStart>\n" +
                "                <AgentHandlers>\n" +
                "                  <ObservationFromFullStats/>\n" +
                "                  <ContinuousMovementCommands turnSpeedDegs=\"180\"/>\n" +
                "                  <InventoryCommands/>\n" +
                "                  <AgentQuitFromReachingPosition>\n" +
                "                    <Marker x=\"-26.5\" y=\"40\" z=\"0.5\" tolerance=\"0.5\" description=\"Goal_found\"/>\n" +
                "                  </AgentQuitFromReachingPosition>\n" +
                "                </AgentHandlers>\n" +
                "              </AgentSection>\n" +
                "            </Mission>";


        AgentHost agent = new AgentHost();
        try {
            StringVector args = new StringVector();
            args.add("Tutorial4");
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

        System.out.println("\n\nMission running\nDon't forget to add your own code! A possible solution is located in \"Tutorial4Solved.java\"");

        // ADD YOUR CODE HERE
        // TO GET YOUR AGENT TO THE DIAMOND BLOCK


        // Loop until mission ends
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
