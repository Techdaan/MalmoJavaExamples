import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.msr.malmo.*;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A "Cliff-walking" example using tabular Q learning
 * This class is based on the Python example: tabular_q_learning.py
 */
public class TabularQLearning {

    static {
        System.loadLibrary("MalmoJava"); // Load the Malmo JNI
    }


    /**
     * Tabular Q-learning agent for discrete state/action spaces.
     */
    public static class TabularQAgent {
        /**
         * All the possible actions we have
         */
        private String[] actions;

        /**
         * The greedy-policy (Epsilon)
         */
        private double epsilon;

        /**
         * The learning rate
         */
        private double alpha;

        /**
         * The value of the future reward
         */
        private double gamma;

        /**
         * Whether if or if not we are still learning
         */
        private boolean training = true;

        /**
         * Whether if or if not to debug
         */
        private boolean debug;

        /**
         * The canvas we should debug on
         */
        private Canvas canvas;

        /**
         * The data we have gathered so far
         */
        private HashMap<String, double[]> qTable = new HashMap<>();

        /**
         * The action we previously
         */
        private String previousAction = null;

        /**
         * The state we previousely had
         */
        private String previousState = null;

        /**
         * Creates a new tabular-q agent
         * @param actions The set of actions we can perform
         * @param epsilon The epsilon, also known as greedy policy - the chance that the agent goes in a random direction instead of the "best", trying to figure out new and better ways.
         * @param alpha The learning rate as described in Barto and Sutton's introduction to RL
         * @param gamma The value of the future reward
         * @param debug Whether or not to debug. True = debug, False = no debug
         * @param canvas The canvas to draw on.
         */
        public TabularQAgent(String[] actions, double epsilon, double alpha, double gamma, boolean debug, Canvas canvas) {
            this.actions = actions;
            this.epsilon = epsilon;
            this.alpha = alpha;
            this.gamma = gamma;
            this.debug = debug;
            this.canvas = canvas;
        }

        /**
         * Runs the agent through one attempt
         * @param agentHost The host of the mission
         * @return The amount of rewards we've collected in this run
         */
        public double run(AgentHost agentHost) {
            double totalReward = 0;
            double currentReward = 0;
            double tol = 0.01;

            this.previousAction = null;
            this.previousAction = null;

            WorldState worldState = agentHost.peekWorldState();
            while(worldState.getIsMissionRunning() && !hasObservations(worldState))
                worldState = agentHost.peekWorldState();

            int framesSeen = worldState.getNumberOfVideoFramesSinceLastState();
            while(worldState.getIsMissionRunning() && worldState.getNumberOfVideoFramesSinceLastState() == framesSeen)
                worldState = agentHost.peekWorldState();

            worldState = agentHost.getWorldState();
            for(int i=0; i<worldState.getErrors().size(); i++)
                System.err.println(worldState.getErrors().get(i));

            if(!worldState.getIsMissionRunning())
                return 0; // Quit if mission ended before it should

            if(worldState.getVideoFrames().size() <= 0) {
                System.err.println("We haven't received any video frames!");
                return 0;
            }

            System.out.println("observation len: "+worldState.getObservations().size());
            JsonObject observation = (JsonObject) new JsonParser().parse(worldState.getObservations().get(0).getText());
            if(!observation.has("XPos") || !observation.has("ZPos")) {
                System.err.println("Received invalid observations - no 'XPos' or 'ZPos' found");
                return 0;
            }
            int previousX = observation.get("XPos").getAsInt();
            int previousZ = observation.get("ZPos").getAsInt();
            System.out.format("Initial position: %d, %d\n", previousX, previousZ);

            totalReward += act(worldState, agentHost, currentReward);

            boolean requireMove = true;
            boolean checkExpectedPosition = true;

            while(worldState.getIsMissionRunning()) {
                System.out.println("Waiting for data...");
                while(true) {
                    worldState = agentHost.peekWorldState();
                    if(!worldState.getIsMissionRunning()) {
                        System.out.println("Mission ended");
                        break;
                    }
                    if(worldState.getRewards().size() > 0 && hasObservations(worldState)) {
                        observation = (JsonObject) new JsonParser().parse(worldState.getObservations().get(0).getText());
                        int currentX = observation.get("XPos").getAsInt();
                        int currentZ = observation.get("ZPos").getAsInt();
                        if(requireMove) {
                            if(Math.hypot(currentX - previousX, currentZ - previousZ) > tol) {
                                System.out.println("received.");
                                break;
                            }
                        } else {
                            System.out.println("received.");
                            break;
                        }
                    }
                }

                framesSeen = worldState.getNumberOfVideoFramesSinceLastState();
                while(worldState.getIsMissionRunning() && worldState.getNumberOfVideoFramesSinceLastState() == framesSeen)
                    worldState = agentHost.peekWorldState();

                long framesBeforeGet = worldState.getVideoFrames().size();

                worldState = agentHost.getWorldState();
                for(int i=0; i<worldState.getErrors().size(); i++)
                    System.err.println(worldState.getErrors().get(i));

                for(int i=0; i<worldState.getRewards().size(); i++)
                    currentReward += worldState.getRewards().get(i).getValue();

                if(worldState.getIsMissionRunning()) {
                    if(worldState.getVideoFrames().size() <= 0) {
                        System.err.println("We haven't received any video frames!");
                        return 0;
                    }
                    long framesAfterGet = worldState.getVideoFrames().size();
                    if(framesAfterGet >= framesBeforeGet) {
                        System.err.println("Fewer frames after getWorldState()?!");
                        //return 0;
                    }

                    observation = (JsonObject) new JsonParser().parse(worldState.getObservations().get(0).getText());
                    int currentX = observation.get("XPos").getAsInt();
                    int currentZ = observation.get("ZPos").getAsInt();
                    System.out.format("New position from observation %d, %d after action '%s'\n", currentX, currentZ, previousAction);
                    if(checkExpectedPosition) {
                        // TODO make this
                    }

                    previousX = currentX;
                    previousZ = currentZ;
                    totalReward += act(worldState, agentHost, currentReward);
                }
            }

            System.out.println("Final reward: "+currentReward);
            totalReward += currentReward;

            if(training && previousState != null && previousAction != null) {
                double oldQ = qTable.get(previousState)[getAction(previousAction)];
                qTable.get(previousState)[getAction(previousAction)] = oldQ + alpha * (currentReward - oldQ);
            }

            drawQ(0, 0);

            return totalReward;
        }

        /**
         * Acts based on the current world state
         * @param worldState The current world state
         * @param agentHost The agent host
         * @param currentReward The reward we've collected so far
         */
        public double act(WorldState worldState, AgentHost agentHost, double currentReward) {
            JsonObject observation = (JsonObject) new JsonParser().parse(worldState.getObservations().get(0).getText());
            if(!observation.has("XPos") || !observation.has("ZPos")) {
                System.err.println("Received invalid observations - no 'XPos' or 'ZPos' found");
                return 0;
            }

            int currentX = observation.get("XPos").getAsInt();
            int currentZ = observation.get("ZPos").getAsInt();
            String currentState = String.format("%d:%d", currentX, currentZ);
            if(debug)
                System.out.format("Debug > State: %s (x=%d, z=%d)\n", currentState, currentX, currentZ);

            if(!qTable.containsKey(currentState))
                qTable.put(currentState, new double[actions.length]);

            // TD(0) algorithm as stated in Barto and Sutton's introduction to Reinforcement Learning 2016 version 2 draft
            if(training && previousState != null && previousAction != null) {
                double oldQ = qTable.get(previousState)[getAction(previousAction)];
                qTable.get(previousState)[getAction(previousAction)] = oldQ + alpha * (currentReward
                        + gamma * getHighest(qTable.get(currentState)) - oldQ);
            }

            drawQ(currentX, currentZ);

            int a;
            double random = Math.random();
            if(random < epsilon) {
                a = ThreadLocalRandom.current().nextInt(0, actions.length);
                System.out.println("Taking random action: "+actions[a]);
            } else {
                double max = getHighest(qTable.get(currentState));
                List<Integer> list = new ArrayList<>();
                for(int x=0; x<actions.length; x++) {
                    double dbl =  qTable.get(currentState)[x];
                    if (qTable.get(currentState)[x] == max)
                        list.add(x);
                }
                a = list.get(ThreadLocalRandom.current().nextInt(0, list.size()));
                System.out.println("Taking q action: "+actions[a]+" (Highest reward: "+max+")");
            }

            // Send the command
            agentHost.sendCommand(actions[a]);
            previousState = currentState;
            previousAction = actions[a];

            return currentReward;
        }

        /**
         * Draws the current path
         * @param currX The current X position
         * @param currY The current Y position
         */
        private void drawQ(int currX, int currY) {
            boolean drawCurrentPos = currX>Integer.MIN_VALUE;
            int scale = 40;
            int worldX = 6;
            int worldY = 14;

            double actionInset = 0.1;
            double actionRadius = 0.1;
            double currRadius = 0.2;
            double[][] actionPositions = new double[][] {
                    {0.5, actionInset},
                    {0.5, 1-actionInset},
                    {actionInset, 0.5},
                    {1-actionInset, 0.5}
            };

            int minValue = -20;
            int maxValue = 20;

            Graphics g = canvas.getGraphics();
            for(int x=0; x<worldX; x++) {
                for (int y = 0; y < worldY; y++) {
                    g.setColor(Color.WHITE);
                    g.fillRect(x * scale, y * scale, (x + 1) * scale, (y + 1) * scale);
                    g.setColor(Color.BLACK);
                    g.fillRect((x * scale) + 1, (y * scale + 1), ((x + 1) * scale) - 1, ((y + 1) * scale) - 1);
                    String state = String.format("%d:%d", x, y);
                    if(qTable.containsKey(state)) {
                        double[] data = qTable.get(state);
                        for(int act=0; act<actions.length; act++) {
                            int value = (int) data[act];
                            double color = 255 * (value-minValue) / (maxValue-minValue);
                            color = Math.max(Math.min(color, 255), 0);
                            Color col = new Color(255 - (int) color, (int) color, 0);
                            g.setColor(col);
                            g.fillOval((int) ((x + actionPositions[act][0] - actionRadius) * scale),
                                    (int) ((y + actionPositions[act][1] - actionRadius) * scale),
                                    (int) ((x + actionPositions[act][0] + actionRadius) * scale) - (int) ((x + actionPositions[act][0] - actionRadius) * scale),
                                    (int) ((y + actionPositions[act][1] + actionRadius) * scale) - (int) ((y + actionPositions[act][1] - actionRadius) * scale));

                        }
                    }
                }
            }

            g.setColor(Color.WHITE);
            if(drawCurrentPos) {
                g.fillOval((int) ((currX + 0.5 - currRadius) * scale),
                        (int) ((currY + 0.5 - currRadius) * scale),
                        (int) (((currY + 0.5 + currRadius) * scale) - ((currY + 0.5 - currRadius) * scale)),
                        (int) (((currX + 0.5 + currRadius) * scale) - ((currX + 0.5 - currRadius) * scale)));
            }
        }

        /* Utility functions */
        /**
         * Checks if the world state has observations
         * @param state The current WorldState
         * @return True if there's observations, false if not
         */
        protected boolean hasObservations(WorldState state) {
            if(state.getObservations().size() == 0)
                return false;
            boolean has = true;
            for(int i=0; i<state.getObservations().size(); i++)
                if(state.getObservations().get(i).getText().equalsIgnoreCase("{}"))
                    has = false;
            return has;
        }

        /**
         * Converts the action into the index based on the action array
         * @param action The action to look up
         * @return The index of the action
         */
        protected int getAction(String action) {
            for(int i=0; i<actions.length; i++) {
                if(actions[i].equals(action))
                    return i;
            }
            System.err.println("Action index not found for action: "+action);
            return 0;
        }

        /**
         * Returns the highest value in the array
         * @param arr The array to scan
         * @return The highest value in the array
         */
        protected double getHighest(double[] arr) {
            double val = Integer.MIN_VALUE;
            for(int i=0; i<arr.length; i++) {
                if(arr[i]>val) {
                    val = arr[i];
                }
            }
            return val;
        }
    }

    public static void main(String[] argv) {
        AgentHost agentHost = new AgentHost();

        // Register possible arguments and their defaults
        agentHost.addOptionalStringArgument("mission_file", "Path/to/file from which to load the mission.", "C:/Malmo/Sample_missions/cliff_walking_1.xml"); // TODO Relative path instead of static default path
        agentHost.addOptionalFloatArgument("alpha", "Learning rate of the Q-learning agent.", 0.1);
        agentHost.addOptionalFloatArgument("epsilon", "Exploration rate of the Q-learning agent.", 0.01);
        agentHost.addOptionalFloatArgument("gamma", "Discount factor.", 1.0);
        agentHost.addOptionalFlag("load_model", "Load initial model from model_file.");
        agentHost.addOptionalStringArgument("model_file", "Path to the initial model file", "");
        agentHost.addOptionalFlag("debug", "Turn on debugging.");

        try {
            StringVector args = new StringVector();
            args.add("Tutorial6");
            for (String arg : argv)
                args.add(arg);
        } catch (Exception e) {
            System.err.format("ERROR: %s", e.getMessage());
            System.err.println(agentHost.getUsage());
        }
        if (agentHost.receivedArgument("help")) {
            System.out.println(agentHost.getUsage());
            System.exit(0);
        }

        // Create debug frame
        JFrame frame = new JFrame("Q-table");
        Canvas canvas = new Canvas();
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Insets insets = frame.getInsets();
        frame.getContentPane().setPreferredSize(new Dimension(insets.left + insets.right + 6*40, insets.top + insets.bottom + 14*40));
        frame.add(canvas);
        frame.pack();
        frame.setVisible(true);

        int numMaps = 30000;
        if (agentHost.receivedArgument("test"))
            numMaps = 1;

        for (int runMap = 0; runMap < numMaps; runMap++) {
            String[] actionSet = new String[]{"movenorth 1", "movesouth 1", "movewest 1", "moveeast 1"};    // Possible actions

            TabularQAgent agent = new TabularQAgent(actionSet,
                    0.01,
                    0.1,
                    1.0,
                    true,
                    canvas);

            MissionSpec mission = null;
            try {
                //String missionFile = agentHost.getStringArgument("mission_file");
                String missionFile = "C:/Malmo/Sample_missions/cliff_walking_1.xml";
                mission = new MissionSpec(readFile(missionFile, Charset.forName("UTF-8")), true);
                mission.removeAllCommandHandlers();
                mission.allowAllDiscreteMovementCommands();
                mission.requestVideo(320, 240);
                mission.setViewpoint(1);
            } catch (Exception e) {
                System.err.println("Error occured: " + e.getMessage());
                e.printStackTrace();
            }

            if (mission == null)
                continue;         // Exit if mission couldn't be initialized - on to the next map TODO Should this exit the application?

            // Draw holes to make it interesting
            for (int z = 2; z < 12; z+=2) {
                int x = ThreadLocalRandom.current().nextInt(1, 4); // Random value between 1 and 3. No, there is no typo. It's supposed to be .nextInt(min, max+1)
                mission.drawBlock(x, 45, z, "lava");
            }

            // Make a client pool
            ClientPool clientPool = new ClientPool();
            clientPool.add(new ClientInfo("127.0.0.1", 10000));
            // Add your own clients here if you wish!

            int maxRetries = 3;     // How many times should we try to restart if we fail to restart
            int agentId = 0;
            String expId = "tabular_q_learning";

            int numRepeats = 10000;   // How many times we should let the agent run on the same map
            List<Double> cumulativeRewards = new ArrayList<>();
            for (int repeat = 0; repeat < numRepeats; repeat++) {

                System.out.format("Map %d - Mission %d out of %d: \n", runMap, repeat + 1, numRepeats);

                MissionRecordSpec missionRecord = new MissionRecordSpec(String.format("./malmosaves/save_%s-map%d-rep%d.tgz", expId, runMap, repeat));
                missionRecord.recordCommands();
                missionRecord.recordMP4(20, 400000);
                missionRecord.recordRewards();
                missionRecord.recordObservations();

                // Try to start the mission
                for(int retry=0; retry<maxRetries; retry++) {
                    try {
                        agentHost.startMission(mission, missionRecord);
                        break; // Mission started - stop trying to start it again
                    } catch(Exception e) {
                        if(retry == maxRetries - 1) {   // Failed to start the mission as many times as we defined in "maxRetries"
                            System.err.println("Fatal error - failed to start the mission multiple times - exiting program");
                            System.exit(1);
                        }

                        System.err.println("Couldn't start mission: "+e.getMessage());
                        e.printStackTrace();
                        sleep(2000);
                    }
                }

                // Wait for the mission to begin
                System.out.println("Waiting for the mission to start");
                WorldState worldState = agentHost.getWorldState();
                while(!worldState.getIsMissionRunning()) {
                    System.out.print(".");
                    sleep(100);
                    worldState = agentHost.getWorldState();
                    for(int i=0; i<worldState.getErrors().size(); i++)
                        System.err.println(worldState.getErrors().get(i).getText());
                }

                double cumulativeReward = agent.run(agentHost);
                System.out.println("Reward received: "+cumulativeReward);
                cumulativeRewards.add(cumulativeReward);

                sleep(500); // Let the agent clean up
            }
        }
    }

    /* Utility functions */

    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch(InterruptedException e) {
            System.err.println("Thread interrupted: "+e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
