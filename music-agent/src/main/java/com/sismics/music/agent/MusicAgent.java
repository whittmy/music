package com.sismics.music.agent;

import java.awt.Desktop;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import com.sismics.music.agent.deployer.DeploymentStatus;
import com.sismics.music.agent.deployer.DeploymentStatusListener;
import com.sismics.music.agent.deployer.MusicDeployer;
import com.sismics.music.agent.model.Setting;
import com.sismics.music.agent.ui.AgentFrame;
import com.sismics.music.agent.ui.TrayController;
import com.sismics.util.EnvironmentUtil;

/**
 * Windows Agent to configure and launch the Reader application.
 *
 * @author jtremeaux
 */
public class MusicAgent {

    private final List<DeploymentStatusListener> listeners = new ArrayList<DeploymentStatusListener>();
    
    private final TrayController trayController;
    
    private AgentFrame frame;
    
    private boolean elevated;
    
    private final MusicDeployer musicDeployer;

    private final Setting setting;
    
    /**
     * Constructor of MusicAgent.
     */
    public MusicAgent() {
        setting = new Setting();
        
        musicDeployer = new MusicDeployer(this);
        setLookAndFeel();
        trayController = new TrayController(this);
    }

    public void notifyDeploymentInfo() {
        final DeploymentStatus status = musicDeployer.getDeploymentStatus();
        if (listeners != null) {
            for (DeploymentStatusListener listener : listeners) {
                listener.notifyDeploymentStatus(status);
            }
        }
    }
    
    private void setLookAndFeel() {
        try {
            if (EnvironmentUtil.isWindows()) {
                UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            }
        } catch (Throwable e) {
            System.err.println("Failed to set look-and-feel.\n" + e);
        }
    }

    /**
     * If necessary, restart agent with elevated rights.
     */
    public void checkElevation(String... args) {

        if (isElevationNeeded() && !isElevated()) {
            try {
                List<String> command = new ArrayList<String>();
                command.add("cmd");
                command.add("/c");
                command.add("music-agent-elevated.exe");
                command.addAll(Arrays.asList(args));

                ProcessBuilder builder = new ProcessBuilder();
                builder.command(command);
                System.err.println("Executing: " + command + " with current dir: " + System.getProperty("user.dir"));
                builder.start();
                System.exit(0);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(frame, "Failed to elevate Music Control Panel. " + e,
                        "Error",
                        JOptionPane.WARNING_MESSAGE);
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * Returns whether UAC elevation is necessary (to start/stop services etc).
     * 
     * @return Elevation needed
     */
    private boolean isElevationNeeded() {
        if (!EnvironmentUtil.isWindows()) {
            return false;
        }
        
        final String osVersion = EnvironmentUtil.getOsVersion();
        try {
            int majorVersion = Integer.parseInt(osVersion.substring(0, osVersion.indexOf(".")));

            // Elevation is necessary in Windows Vista (os.version=6.1) and later.
            return majorVersion >= 6;
        } catch (Exception e) {
            System.err.println("Failed to resolve OS version from '" + osVersion + "'\n" + e);
            return false;
        }
    }

    /**
     * Add a deployment status listener.
     * 
     * @param listener Listener
     */
    public void addListener(DeploymentStatusListener listener) {
        listeners.add(listener);
    }

    public void showStatusPanel() {
        frame.showStatusPanel();
    }

    public void showTrayIconMessage() {
        trayController.showMessage();
    }

    public void exit() {
        trayController.uninstallComponents();
        System.exit(0);
    }

    /**
     * Open Music in a browser window.
     */
    public void openBrowser() {
        try {
            Desktop.getDesktop().browse(new URI(musicDeployer.getUrl()));
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /**
     * Start the agent.
     * 
     * @param args Arguments
     */
    private void start(List<String> args) {
        elevated = args.contains("-elevated");
        frame = new AgentFrame(this);

        if (args.contains("-balloon")) {
            showTrayIconMessage();
        }

        if (setting.isAutoStart()) {
            checkElevation("-start");
            musicDeployer.start();
        } else if (args.contains("-start")) {
            System.out.println("Starting service");
            musicDeployer.start();
            showStatusPanel();
        } else if (args.contains("-stop")) {
            System.out.println("Stopping service");
            musicDeployer.stop();
            showStatusPanel();
        } 
    }

    /**
     * Entry point of the Windows Agent.
     * 
     * @param args Arguments
     */
    public static void main(String[] args) {
        System.out.println("Starting up Windows agent");

        MusicAgent agent = new MusicAgent();
        agent.start(Arrays.asList(args));
    }

    /**
     * Getter of musicDeployer.
     *
     * @return musicDeployer
     */
    public MusicDeployer getMusicDeployer() {
        return musicDeployer;
    }

    /**
     * Getter of elevated.
     * 
     * @return elevated
     */
    private boolean isElevated() {
        return elevated;
    }

    /**
     * Getter of setting.
     *
     * @return setting
     */
    public Setting getSetting() {
        return setting;
    }    
}
