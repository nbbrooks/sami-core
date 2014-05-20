package sami.ui;

import java.awt.*;
import java.io.*;
import java.util.logging.Logger;

/** * * @author  Jijun Wang */
public class FrameManager extends javax.swing.JFrame {

    private static final Logger LOGGER = Logger.getLogger(FrameManager.class.getName());
    private static String confName = "fManager.ini";
    /** In one applicationn, we only need one frame manager */
    private static boolean existed = false;
    private long startTime;
    private javax.swing.JLabel jTime;

    public FrameManager() {
        if (existed) {
            return;
        }
        existed = true;
        javax.swing.JButton jSave = new javax.swing.JButton();
        jSave.setText("Save");
        jSave.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                saveLayout();
            }
        });
        javax.swing.JButton jRestore = new javax.swing.JButton();
        jRestore.setText("Restore");
        jRestore.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                restoreLayout();
            }
        });
        javax.swing.JButton jClose = new javax.swing.JButton();
        jClose.setText("Close");
        jClose.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent event) {
                closeAll();
            }
        });
        jTime = new javax.swing.JLabel("Time", javax.swing.JLabel.CENTER);
        jTime.setForeground(Color.BLUE);
        jTime.setFont(new Font("Serif", Font.BOLD, 18));
        startTime = System.currentTimeMillis();
        (new Thread(new Runnable() {

            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                    long currentTime = System.currentTimeMillis();
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("HH:mm:ss");
                    dateFormat.setTimeZone(java.util.TimeZone.getTimeZone("GMT"));
                    long elapsed = currentTime - startTime;
                    jTime.setText("Time " + dateFormat.format(new java.util.Date(elapsed)));
                }
            }
        })).start();
        setTitle("FrameManager");
        getContentPane().add(jSave, java.awt.BorderLayout.WEST);
        getContentPane().add(jRestore, java.awt.BorderLayout.CENTER);
        getContentPane().add(jClose, java.awt.BorderLayout.EAST);
        getContentPane().add(jTime, java.awt.BorderLayout.SOUTH);
        pack();
        setAlwaysOnTop(true);
        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
    }

    public static void saveLayout() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(confName));
            Frame[] frames = Frame.getFrames();
            String str;
            Rectangle rec;
            for (int i = 0; i < frames.length; i++) {
                rec = frames[i].getBounds();
                str = "" + i + ":" + frames[i].getTitle() + ":" + rec.x + ":" + rec.y + ":" + rec.width + ":" + rec.height + "\n";
                bw.write(str, 0, str.length());
            }
            bw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void restoreLayout() {
        Frame[] frames = Frame.getFrames();
        try {
            BufferedReader br = new BufferedReader(new FileReader(confName));
            String line;
            FrameLayout fl;
            while ((line = br.readLine()) != null) {
                fl = FrameLayout.parseFrameLayout(line);
                if (fl == null || fl.id >= frames.length) {
                    continue;
                }
                if (fl.name.equals(frames[fl.id].getTitle())) {
                    frames[fl.id].setBounds(fl.rec);
                }
            }
        } catch (FileNotFoundException fe) {
            LOGGER.warning("Could not find FrameManager layout configuration file " + confName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void closeAll() {
        Frame[] frames = Frame.getFrames();
        for (int i = 0; i < frames.length; i++) {
            frames[i].dispose();
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        javax.swing.JFrame[] fs = new javax.swing.JFrame[5];
        for (int i = 0; i < 5; i++) {
            fs[i] = new javax.swing.JFrame();
            fs[i].setTitle("Frame" + i);
            fs[i].setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
            fs[i].setVisible(true);
        }
        FrameManager fm = new FrameManager();
        FrameManager.restoreLayout();
        fm.setVisible(true);
    }
}

class FrameLayout {

    int id;
    String name;
    Rectangle rec;

    public FrameLayout(int id, String name, Rectangle rec) {
        this.id = id;
        this.name = name;
        this.rec = rec;
    }

    public static FrameLayout parseFrameLayout(String str) {
        String[] params = str.split(":");
        if (params.length != 6) {
            return null;
        }
        int id = Integer.parseInt(params[0]);
        String name = params[1];
        int x = Integer.parseInt(params[2]);
        int y = Integer.parseInt(params[3]);
        int width = Integer.parseInt(params[4]);
        int height = Integer.parseInt(params[5]);
        return new FrameLayout(id, name, new Rectangle(x, y, width, height));
    }
}