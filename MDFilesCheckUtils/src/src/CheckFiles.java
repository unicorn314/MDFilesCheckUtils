package src;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import checker.WrongPathChecker;

/**
 * 低检工具主类.
 * @author 123
 *
 */
public class CheckFiles {
    /**
     * 存放所有失效的引用链接.
     */
    private List<MyURL> badURLList = new ArrayList<MyURL>();
    /**
     * 存放所有错误的include标签内引用路径.
     */
    private List<MyURL> wrongIncludePathList = new ArrayList<MyURL>();
    /**
     * 存放所有错误的内部引用路径.
     */
    private List<MyURL> wrongInternalPathList = new ArrayList<MyURL>();
    /**
     * 存放所有字符编码不为UTF-8格式的文件.
     */
    private List<MyURL> wrongCharsetFile = new ArrayList<MyURL>();
    /**
     * 存放所有title部分格式不正确的文件.
     */
    private List<MyURL> wrongTitleFile = new ArrayList<MyURL>();
    /**
     * 存放最终输出结果的字符串.
     */
    private String resultStr = "";


    /**
     * 默认构造方法，所有窗口组件初始化
     */
    public CheckFiles() {
    	init();
    }


    /**
     * 失效的引用链接.
     * @return badURLList
     */
    public List<MyURL> getBadURLList() {
        return badURLList;
    }
    /**
     * 错误的include标签内引用路径.
     * @return wrongIncludePathList
     */
    public List<MyURL> getWrongIncludePathList() {
        return wrongIncludePathList;
    }
    /**
     * 错误的内部引用路径.
     * @return wrongInternalPathList
     */
    public List<MyURL> getWrongInternalPathList() {
        return wrongInternalPathList;
    }
    /**
     * 字符编码不为UTF-8格式的文件.
     * @return wrongCharsetFile
     */
    public List<MyURL> getWrongCharsetFile() {
        return wrongCharsetFile;
    }
    /**
     * title部分格式不正确的文件.
     * @return wrongTitleFile
     */
    public List<MyURL> getWrongTitleFile() {
        return wrongTitleFile;
    }
    /**
     * 结果字符串.
     * @return resultStr
     */
    public String getResultStr() {
        return resultStr;
    }

    /**
     * 递归查找指定路径下所有的.md和.yml文件.
     * @param folder 指定搜索的文件夹
     * @return 所有的文件数组
     */
    public File[] searchFile(final File folder) {
        try {
            File[] subFolders = folder.listFiles();
            List<File> result = new ArrayList<File>(); // 声明一个集合
            for (int i = 0; i < subFolders.length; i++) { // 循环显示文件夹或文件
                if (subFolders[i].isFile()) { // 如果是文件,下一步判断
                    if (subFolders[i].getName()
                            .toLowerCase().endsWith(".md")
                            || subFolders[i].getName()
                            .toLowerCase()
                            .endsWith(".yml")) {
                        // 如果是.md文件，则将结果加入列表，否则不做处理
                        result.add(subFolders[i]);
                    }
                } else { // 如果是文件夹，则递归调用本方法，然后把所有的文件加到结果列表中
                    File[] foldResult = searchFile(subFolders[i]);
                    // 循环显示文件
                    for (int j = 0; j < foldResult.length; j++) {
                        result.add(foldResult[j]); // 文件保存到集合中
                    }
                }
            }
            File[] files = new File[result.size()]; // 声明文件数组，长度为集合的长度
            result.toArray(files); // 集合数组化
            return files;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 判引入的网页断链接是否有效.
     * @param strLink 输入链接
     * @return 返回true或者false 　　
     */
    public boolean isValidURL(final String strLink) {
        URL url;
        try {
            url = new URL(strLink);
            HttpURLConnection connt = (HttpURLConnection) url.openConnection();
            connt.setRequestMethod("HEAD");
            String strMessage = connt.getResponseMessage();
            if (strMessage.compareTo("Not Found") == 0) {
                return false;
                }
            connt.disconnect();
            } catch (Exception e) {
                return false;
            }
        return true;
    }

    /**
     * 判断include标签中的引用路径是否有效.
     * @param rootPath 项目根路径
     * @param strPath 引用路径
     * @return 返回true或者false 　　
     */
    public boolean isValidPath(final String rootPath,
            final String strPath) {
        String include;
        Pattern pattern = Pattern.compile("/$");
        Matcher matcher = pattern.matcher(rootPath);
        if (matcher.find()) {
            include = "_include/";
        } else {
            include = "/_include/";
        }
        File folder = new File(rootPath + include + strPath);
        if (folder.exists()) {
            return true;
        }
        return false;
    }

    /**
     * 在文件中搜索失效链接.
     * @param file 输入需要检索的文件
     */
    public void searchBadURL(final File file) {
        // 网址正则判断
        String rex = "(http://|ftp://|https://)"
                + "[^\u4e00-\u9fa5\\s\"\'\\()]*?\\."
                + "(com|net|cn|me|tw|fr|edu)[^\u4e00-\u9fa5\\s\"\'\\()]*";
        Pattern pattern = Pattern.compile(rex);

        //行读取
        LineNumberReader lineReader = null;
        try {
            lineReader = new LineNumberReader(new FileReader(file));
            String readLine = null;
            while ((readLine = lineReader.readLine()) != null) { // 读取文件内容
                Matcher matcher = pattern.matcher(readLine);
                while (matcher.find()) { // 假如这一行存在可以匹配网址正则表达式的字符串
                    if (!isValidURL(matcher.group(0))) { // 检测网址是否可用
                        MyURL myURL = new MyURL();
                        myURL.setFile(file.getParent()
                                + "\\" + file.getName());
                        myURL.setUrl(matcher.group(0));
                        // 若网址不可用，将这个网址所属的文件名和网址自身插入到失效链接列表
                        badURLList.add(myURL);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //关闭流
            close(lineReader);
        }
    }

    /**
     * 在文件中搜索include标签中错误的引用文件路径.
     * @param file 需要检测的文件
     * @param rootPath 项目根路径
     */
    public void searchWrongIncludePath(final File file, final String rootPath) {
        // include正则判断
        String rex = "\\{%[\\s]+include[\\s]+[/]?"
                + "[a-zA-z0-9\\/\\.]*[\\s]+%\\}";
        Pattern pattern = Pattern.compile(rex);

        LineNumberReader lineReader = null;
        try {
            lineReader = new LineNumberReader(new FileReader(file));
            String readLine = null;

            // 读取文件内容
            while ((readLine = lineReader.readLine()) != null) {
                Matcher matcher = pattern.matcher(readLine);
                while (matcher.find()) { // 假如这一行存在可以匹配include标签正则表达式的字符串
                    // 将路径字符串从include标签字符串中切割出来
                    String includePath = matcher.group(0).replaceAll(
                            "\\{%[\\s]+include[\\s]+", "")
                            .replaceAll("[\\s]+%\\}", "");

                    // 检测路径是否可用
                    if (!isValidPath(rootPath, includePath.trim())) {
                        MyURL myURL = new MyURL();
                        myURL.setFile(file.getParent()
                                + "\\" + file.getName());
                        myURL.setUrl(includePath);
                        // 若路径不可用，将这个网址所属的文件名和网址字符串插入到错误路径list
                        wrongIncludePathList.add(myURL);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //关闭流
            close(lineReader);
        }
        }

    /**
     * 检查title部分内容是否符合格式.
     * @param file 需要检测的文件
     */
    public void searchWrongTitle(final File file) {
        /*
         * 检查内容：
         * 1.title标识线是否为3个"-"
         * 2.title内容里面key和value中间的冒号后面是否跟有空格
         */
        LineNumberReader lineReader = null;
        int titleLineNum = 0; // 已读取到的title标识线"---"行的数量
        int titleContentNum = 0; // 已读取到的title内容"key: value"行的数量
        try {
            lineReader = new LineNumberReader(new FileReader(file));
            String readLine = null;
            while ((readLine = lineReader.readLine()) != null) {
                // 从文件第一行开始读取，直到最后一行
                if (readLine.matches("\\s*")) { // 假如是空行，则继续读取下一行
                    continue;
                } else if (titleLineNum == 0 && !readLine.matches("^[\\-]+$")) {
                    // 如果读到的第一行不是若干个"-"组成的，说明此文件没有加title，跳过此文件
                    break;
                } else if (titleLineNum == 0
                        && !readLine.matches("^[\\-]{3}$")) {
                    // 如果第一行"-"数量不为3个，则记入title错误文件的list
                    MyURL myURL = new MyURL();
                    myURL.setFile(file.getParent() + file.getName());
                    wrongTitleFile.add(myURL);
                    break;
                } else if (titleLineNum == 0
                        && readLine.matches("^[\\-]{3}$")) {
                    // 如果开头的"-"数量为3个，则继续读取文件，检查title内容
                    titleLineNum++; // title标识线数量+1，表明已经找到title的开头
                    continue;
                } else if (titleLineNum == 1) {
                    // 如果已经找到上半截title标识线，检测title内容是否符合格式
                    if (readLine.matches(".*[\\:]{1}[\\s]{1}.*")) {
                        // 如果该行内容符合title内容格式，继续读取下一行内容
                        titleContentNum++;
                        continue;
                    } else if (readLine.matches("\\s*")) {
                        // 如果该title内容行为空行，继续读取下一行内容
                        continue;
                    } else if (titleContentNum > 0
                            && readLine.matches("^[\\-]{3}$")) {
                        // 如果title内容行数不为0的情况下读取到了title的结束标识线，
                        // 则表示title部分读取完毕，跳过该文件剩余部分
                        break;
                    } else {
                        // 如果为其他情况,则记入title错误文件的list
                        MyURL myURL = new MyURL();
                        myURL.setFile(file.getParent()
                            + "\\" + file.getName());
                        wrongTitleFile.add(myURL);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //关闭流
            close(lineReader);
        }
    }

    /**
     * 检测文件编码是否为UTF-8.
     * @param file 输入文件
     * @throws FileNotFoundException 文件不存在
     * @throws IOException IO错误
     */
    public void checkCharset(final File file)
            throws FileNotFoundException, IOException {
        String charset
            = new FileCharsetDetector().guessFileEncoding(file);
        if (!charset.equals("UTF-8")) {
            MyURL myURL = new MyURL();
            myURL.setFile(file.getParent() + "\\" + file.getName());
            myURL.setUrl(charset);
            // 将编码不为UTF-8的文件路径和编码格式记入对应list
            wrongCharsetFile.add(myURL);
        }
    }

    /**
     * 关闭流.
     * @param able 需要关闭的流
     */
    private void close(final Closeable able) {
        if (able != null) {
            try {
                able.close();
            } catch (IOException e) {
                e.printStackTrace();
//                able = null;
            }
        }
    }

    /**
     * 启动线程，开始检测文件.
     * @param rootPath 项目根路径
     */
    public void check(final String rootPath) {
        File folder = new File(rootPath); // 指定项目根目录
        if (!folder.exists()) { // 如果文件夹不存在
            resultStr = resultStr + "目录不存在："
                + folder.getAbsolutePath() + "\r\n";
            return;
        }
        File[] result = searchFile(folder); // 调用方法获得文件数组
        resultStr = resultStr + "在 " + folder
                + " 以及所有子文件时查找对象.md文件及.yml文件" + "\r\n";
        ProGressWork work = new ProGressWork(result,rootPath);
        // 监听器，当值变化时同步更新进度条
        /*work.addPropertyChangeListener(new PropertyChangeListener(){
            @Override  
            public void propertyChange(PropertyChangeEvent evt) {
                if ("progress".equals(evt.getPropertyName())) {
                    jpb.setValue((Integer)evt.getNewValue());
                }
            }
        });*/
        work.execute();
    }

	final int tWidth = 0;
	final int tHeight = 0;
	final int fWidth = 920;
	final int fHeight = 570;
	final int fontSize = 20;
	// 新建主窗口
	JFrame frame = new JFrame();
	final JLabel label = new JLabel();
	final JProgressBar jpb = new JProgressBar();
	JTextArea textArea = new JTextArea(tWidth, tHeight);

	// 文件选择按钮
	final JButton selectFileButton = new JButton("select file");
	// 开始检测按钮
	final JButton checkButton = new JButton("start check");

	/**
	 * 窗口组件初始化
	 */
	private void init() {
		frame.setSize(fWidth, fHeight);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setTitle("低检工具");
		frame.setLayout(new BorderLayout());

		// 主窗口center部分（中间结果显示文本域）
		textArea.setFont(new Font("黑体", Font.BOLD, fontSize));
		textArea.setLineWrap(true);
		JScrollPane scrollPane = new JScrollPane(textArea);
		frame.add(scrollPane, BorderLayout.CENTER);

		// 主窗口south部分（底部进度条）
		JPanel stp = new JPanel();
		stp.add(jpb);
		stp.add(label);
		frame.add(stp, BorderLayout.SOUTH);

		// 主窗口north部分按钮功能（顶部按钮及文件夹路径显示框）
		JPanel northPanle = new JPanel();
		selectFileButton.setMaximumSize(new Dimension(90, 30));
		checkButton.setMaximumSize(new Dimension(90, 30));
		// 文件夹路径显示框
		final TextField rootFolderTextField = new TextField(50);
		rootFolderTextField.setFont(new Font("黑体", Font.LAYOUT_LEFT_TO_RIGHT,
				fontSize));
		rootFolderTextField.setEditable(false);
		northPanle.add(selectFileButton);
		northPanle.add(checkButton);
		northPanle.add(rootFolderTextField);

		// 弹出文件选择窗口
		final JFileChooser chooser = new JFileChooser();
		chooser.setApproveButtonText("确定");
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY); // 设置只选择目录
		// 文件选择按钮添加点击弹出选择文件窗口事件
		selectFileButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int returnVal = chooser.showOpenDialog(chooser);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					// 选择文件夹后，将所选文件夹路径显示到主窗口顶部文本框中
					rootFolderTextField.setText(chooser.getSelectedFile()
							.getPath());
					rootFolderTextField.validate();
				}
			}
		});
		// 开始检查按钮添加点击开始扫描文件夹事件
		checkButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (rootFolderTextField.getText() != null
						&& !"".equals(rootFolderTextField.getText())) {
					// 如果已经选择过文件夹，开始检测
					check(rootFolderTextField.getText());
					// 清空所有错误列表和结果显示界面
					cleanAllLists();
					textArea.setText("");
					checkButton.setEnabled(false);
				} else {
					// 如果还没有选择过文件夹，弹出提示窗口
					JOptionPane.showMessageDialog(null, "请选择项目根路径!");
				}
			}
		});
		frame.add(northPanle, BorderLayout.NORTH);
		frame.setVisible(true);
		// 将结果写入页面中间文本框，并刷新窗口显示内容
		textArea.setText(resultStr);
		textArea.validate();
	}

	/**
	 * 清空所有list
	 */
	public void cleanAllLists() {
		badURLList = new ArrayList<MyURL>();
		wrongIncludePathList = new ArrayList<MyURL>();
		wrongInternalPathList = new ArrayList<MyURL>();
		wrongCharsetFile = new ArrayList<MyURL>();
		wrongTitleFile = new ArrayList<MyURL>();
		resultStr = "";
	}

	/**
	 * 生成文件选择窗口和结果显示窗口.
	 * 
	 * @param args
	 *            ...
	 */
	public static void main(final String[] args) { // java程序的主入口处
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new CheckFiles();
			}
		});
	}

	class ProGressWork extends SwingWorker<List<File>, File> {
		private List<File> result;
		private String rootPath;

		public ProGressWork(File[] files, String path) {
			super();
			result = Arrays.asList(files);
			rootPath = path;
		}

		/**
		 * 启动该更新进度条的线程，开始检测所有文件，并同步刷新进度条
		 */
		@Override
		protected List<File> doInBackground() throws Exception {
			/*
			 * 检查项目包含： 1.所有.md文件及.yml文件中的网页链接是否有效 2.所有include标签中的引用文件路径是否有效
			 * 3.所有文件的编码格式是否为UTF-8编码 4.文件头title的区域标识线是否为3个减号，有没有多写或者少写
			 * 5.文件头title的内容部分，键值对之间的冒号后是否有加上空格 6.所有引入的内部路径是否正确
			 */
			WrongPathChecker wrongPathChecker = new WrongPathChecker();
			for (int i = 0; i < result.size(); i++) { // 循环显示文件
				File file = result.get(i);
				try {
					searchBadURL(file);
					searchWrongIncludePath(file, rootPath);
					checkCharset(file);
					searchWrongTitle(file);
					List<MyURL> wrongIntercalPath = wrongPathChecker
							.searchWrongIntercalPath(file);
					wrongInternalPathList.addAll(wrongIntercalPath);
					jpb.setValue(100 * (i + 1) / result.size());
					// 用于监听器取值
					// setProgress(100 * (i+1) / result.size());
					label.setText((i + 1) + " / " + result.size());
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return result;
		}

		/**
		 * 将所有错误列表中的检测结果拼成字符串，用于页面展示
		 */
		@Override
		protected void done() {
			// 所有检查结果都已存入各自的list，开始将最终结果转换为字符串
			if (badURLList.size() > 0) {
				resultStr = resultStr + "\r\n以下为失效链接：" + "\r\n";
				for (MyURL myURL : badURLList) {
					resultStr = resultStr + "文件路径：" + myURL.getFile() + "        链接地址："
							+ myURL.getUrl() + "\r\n";
				}
			}

			if (wrongIncludePathList.size() > 0) {
				resultStr = resultStr + "\r\n以下为错误include标签引用路径：" + "\r\n";
				for (MyURL myURL : wrongIncludePathList) {
					String url = myURL.getUrl();
					try {
						url = new String(url.getBytes(), "utf-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					resultStr = resultStr + "文件路径：" + myURL.getFile() + "        引用路径："
							+ url + "\r\n";
				}
			}

			if (wrongInternalPathList.size() > 0) {
				resultStr = resultStr + "\r\n以下为错误内部引用路径：" + "\r\n";
				for (MyURL myURL : wrongInternalPathList) {
					String url = myURL.getUrl();
					try {
						url = new String(url.getBytes(), "utf-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					resultStr = resultStr + "文件路径：" + myURL.getFile() + "        引用路径："
							+ url + "\r\n";
				}
			}

			if (wrongCharsetFile.size() > 0) {
				resultStr = resultStr + "\r\n以下为字符编码不正确的文件：" + "\r\n";
				for (MyURL myURL : wrongCharsetFile) {
					String url = myURL.getUrl();
					try {
						url = new String(url.getBytes(), "utf-8");
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}
					resultStr = resultStr + "文件路径：" + myURL.getFile() + "        编码格式："
							+ url + "\r\n";
				}
			}

			if (wrongTitleFile.size() > 0) {
				resultStr = resultStr + "\r\n以下为title部分格式不正确的文件：" + "\r\n";
				for (MyURL myURL : wrongTitleFile) {
					resultStr = resultStr + "文件路径：" + myURL.getFile() + "\r\n\r\n";
				}
			}
			// 将结果保存到日志文件中
			try {
				PrintStream mytxt = new PrintStream("./MDFilesCheckLog.log");
				PrintStream out = System.out;
				System.setOut(mytxt);
				System.out.println(resultStr);
				System.setOut(out);
				System.out.println("日志保存完毕。");
				mytxt.close();
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			// 在文本框中显示结果
			resultStr += "日志文件MDFilesCheckLog.log已生成，保存在当前目录下";
			textArea.setText(resultStr);
			checkButton.setEnabled(true);
		}
	}

}
