package src;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class MySwingWorker extends SwingWorker<Boolean, Integer> {
	private static int max = 0;  
	private static int now = 0;
    // 显示进度条状态的label  
    private JLabel status;  
    // 进度条  
    private JProgressBar jpb;  
  
    public static void main(String[] args) {  
        JFrame frame = new JFrame();
        frame.setTitle("Test Frame");    
        frame.setSize(800,600);    
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  
        final JLabel label = new JLabel();  
//      JScrollPane sp = new JScrollPane(panel);  
//      sp.setSize(new Dimension(700, 500));  
//      frame.add(sp, BorderLayout.CENTER);  
  
        JPanel stp = new JPanel();  
        final JProgressBar jpb = new JProgressBar();  
        jpb.setMinimum(1);  
        jpb.setMaximum(max);  
        stp.add(jpb);  
        stp.add(label);  
        frame.add(stp, BorderLayout.SOUTH);  
  
        JButton button = new JButton("Begin");
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                MySwingWorker msw = new MySwingWorker(label, jpb ,now);
                msw.execute();
            }  
        });  
  
        frame.add(button, BorderLayout.NORTH);  
  
        frame.setVisible(true);  
    }  

    public MySwingWorker(JLabel status, JProgressBar jpb, Integer now) {  
        super();  
        this.status = status;  
        this.jpb = jpb;
        this.now = now;
    }  
  
    /* 
     * doInBackground是工作线程，他可以明确调用publich方法（注意publish方法只在SwingWorker类中实现）， 
     * 以发送中间结果V，然后这个中间结果有被发送到在EDT（事件派发线程）中的 process方法中进行处理。 
     */  
    @Override  
    protected Boolean doInBackground() throws Exception {  
        // TODO Auto-generated method stub  
        /*for (int i = 1; i <= max; i++) {  
            jpb.setValue(i);  
            status.setText(i + " / " + max);  
            try {  
                Thread.sleep(50);  
            } catch (InterruptedException e1) {  
                // TODO Auto-generated catch block  
                e1.printStackTrace();  
            }  
        }  */
    	while (now != max) {
    		jpb.setValue(now);  
            status.setText(now + " / " + max);
    		try {  
                Thread.sleep(100);  
            } catch (InterruptedException e1) {  
                // TODO Auto-generated catch block  
                e1.printStackTrace();  
            }
    	}
        return true;  
    }  

    
    /* 
     * 当doInBackground处理完后，会自动调用done方法，由T类型的描述那里可以知道，在这个方法中可以调用get方法获取最终结果集 
     */  
    @Override  
    protected void done() {  
        try {  
            Boolean result = get();  
            if (result) {  
                JOptionPane.showMessageDialog(null, "Success!");  
            } else {  
                JOptionPane.showMessageDialog(null, "Failed!");  
            }  
        } catch (InterruptedException | ExecutionException e) {  
            // TODO Auto-generated catch block  
            e.printStackTrace();  
        }  
    }
    
    public void setMaxValue(int maxNum) {
    	this.max = maxNum;
    	jpb.setMaximum(maxNum);
    }
    
    public void setNowValue(int nowNum) {
    	this.now = nowNum;
    	jpb.setMinimum(nowNum);
    }
}
