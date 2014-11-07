package com.professionallyevil.co2.laudanum;

import burp.*;
import com.professionallyevil.co2.Co2Configurable;
import com.professionallyevil.co2.Co2Extender;
import com.professionallyevil.co2.Co2HelpLink;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class LaudanumClient implements Co2Configurable, ClipboardOwner, IContextMenuFactory {
    private JPanel mainPanel;
    private JTextField txtAllowedToken;
    private JTextField txtAllowedIP;
    private JButton btnGenerate;
    private JComboBox cmboMethod;
    private JTextField txtHostname;
    private JTextField txtPostBody;
    private JTextArea txtConsole;
    private JComboBox cmboProtocol;
    private JTextField txtPort;
    private JTextField txtResource;
    private JComboBox cmboFiletype;
    private JButton btnSave;
    private JButton btnConnect;
    private JComboBox cmboPrepend;
    private JLabel helpButton;
    private JCheckBox chkUseRequestTemplate;
    private JLabel lblRequestTemplate;
    private String cwd = ".";
    private int commandStart = 0;
    private IBurpExtenderCallbacks callbacks;
    private static final String SETTING_LAUD_TOKEN = "LAUD.TOKEN";
    private static final String SETTING_LAUD_IP = "LAUD.IP";
    private byte[] requestTemplate = null;
    private Co2Extender extender;
    final java.util.List<String> history = new ArrayList<String>();
    int historyPointer = 0;


    final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

    public LaudanumClient(final Co2Extender extender) {
        this.extender = extender;
        final Map<String, PayloadType> payloadTypes = new HashMap<String, PayloadType>();
        payloadTypes.put("PHP Shell", new PHPShellPayloadType());
        payloadTypes.put("JSP Shell", new JSPShellPayloadType());
        payloadTypes.put("WAR Shell", new WARShellPayloadType());
        payloadTypes.put("ASP Shell", new ASPShellPayloadType());
        payloadTypes.put("ASPX Shell", new ASPXShellPayloadType());

        this.callbacks = extender.getCallbacks();
        String token = callbacks.loadExtensionSetting(SETTING_LAUD_TOKEN);
        if (token != null && token.length() > 0) {
            txtAllowedToken.setText(token);
        }
        String ips = callbacks.loadExtensionSetting(SETTING_LAUD_IP);
        if (ips != null && ips.length() > 0) {
            txtAllowedIP.setText(ips);
        }

        //showPrompt();
        ((AbstractDocument) txtConsole.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                if (checkCommandPosition(offset)) {
                    super.remove(fb, offset, length);
                }
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                //if(checkCommandPosition(offset)) {
                super.replace(fb, offset, length, text, attrs);
                //}
            }

            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                super.insertString(fb, offset, string, attr);
            }

            private boolean checkCommandPosition(int offset) {
                return offset >= commandStart;
            }
        });

        txtConsole.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    int currentPosition = txtConsole.getCaretPosition();
                    try {
                        String command = txtConsole.getText(commandStart, (currentPosition - commandStart - 1));

                        runCommand(command, false);
                    } catch (BadLocationException e1) {
                        callbacks.printError(e1.toString());
                    } catch (MalformedURLException e1) {
                        callbacks.printError(e1.toString());
                    } catch (NumberFormatException e1) {
                        callbacks.printError(e1.toString());
                    }
                } else {
                    super.keyReleased(e);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    // TODO: handle history - this gets detected but not interrupted
                } else {
                    super.keyPressed(e);
                }
            }


        });
        txtConsole.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                if (txtConsole.getSelectedText().length() > 0) {
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    StringSelection contents = new StringSelection(txtConsole.getSelectedText());
                    clipboard.setContents(contents, LaudanumClient.this);
                }
                txtConsole.setCaretPosition(txtConsole.getText().length());
            }
        });
        btnGenerate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Random r = new Random();
                byte[] bytes = new byte[20];
                r.nextBytes(bytes);
                txtAllowedToken.setText(bytesToHex(bytes));
            }
        });
        btnSave.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PayloadType pt = payloadTypes.get(cmboFiletype.getSelectedItem().toString());
                try {
                    pt.savePayload(mainPanel, txtAllowedIP.getText(), txtAllowedToken.getText());
                } catch (Exception e1) {
                    callbacks.printError("Error saving payload: " + e1.getMessage());
                    JOptionPane.showMessageDialog(mainPanel, "Error saving payload. " + e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        cmboMethod.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtPostBody.setEnabled(cmboMethod.getSelectedItem().equals("POST"));
            }
        });
        btnConnect.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    txtConsole.setText("Connecting to " + cmboProtocol.getSelectedItem().toString() + "://" + txtHostname.getText() + ":" + txtPort.getText() + txtResource.getText() + ".....\nwhoami\n");
                    runCommand("whoami", true);
                } catch (MalformedURLException e1) {
                    txtConsole.setText("Malformed URL.  Check your hostname and port.");
                    e1.printStackTrace();
                }
            }
        });

        txtAllowedToken.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                callbacks.saveExtensionSetting(SETTING_LAUD_TOKEN, txtAllowedToken.getText());
            }
        });

        txtAllowedIP.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                callbacks.saveExtensionSetting(SETTING_LAUD_IP, txtAllowedIP.getText());
            }
        });

        cmboProtocol.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (cmboProtocol.getSelectedItem().equals("http") && txtPort.getText().equals("443")) {
                    txtPort.setText("80");
                } else if (cmboProtocol.getSelectedItem().equals("https") && txtPort.getText().equals("80")) {
                    txtPort.setText("443");
                }
            }
        });
        helpButton.addMouseListener(new Co2HelpLink("https://code.google.com/p/burp-co2/wiki/Laudanum", helpButton));

        chkUseRequestTemplate.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                cmboProtocol.setEnabled(!chkUseRequestTemplate.isSelected());
                txtPort.setEnabled(!chkUseRequestTemplate.isSelected());
                txtResource.setEnabled(!chkUseRequestTemplate.isSelected());
                txtHostname.setEnabled(!chkUseRequestTemplate.isSelected());
            }
        });
    }


    private void runCommand(String command, boolean muteOutput) throws MalformedURLException {
        if (command.equals("clear")) {
            txtConsole.setText("");
            commandStart = 0;
        } else if (command.equals("cd ..")) {
            int i = cwd.lastIndexOf('/');
            if (i == 0) {
                cwd = "/";
            } else if (i > -1) {
                cwd = cwd.substring(0, i);
            } else {
                i = cwd.lastIndexOf('\\');
                if (i > 0) {
                    cwd = cwd.substring(0, i);
                    if (cwd.indexOf('\\') == -1) {
                        cwd = cwd + '\\';
                    }
                }
            }
        } else {

            URL url = new URL(cmboProtocol.getSelectedItem() + "://" + txtHostname.getText() + ":" + txtPort.getText() + txtResource.getText());

            LaudanumRequest lreq;
            if (requestTemplate != null && chkUseRequestTemplate.isSelected()) {
                lreq = new LaudanumRequest(callbacks, cmboMethod.getSelectedItem().toString(), requestTemplate);
            } else {
                lreq = new LaudanumRequest(callbacks, url, cmboMethod.getSelectedItem().toString());
            }
            lreq.setCommand(command.startsWith("cd ") ? command : getPrepend() + command);
            lreq.setToken(txtAllowedToken.getText());
            lreq.setWorkingDirectory(cwd);

            //TODO: fix error output on PHP error

            byte[] responseBytes = callbacks.makeHttpRequest(txtHostname.getText(), new Integer(txtPort.getText()), "https".equalsIgnoreCase(cmboProtocol.getSelectedItem().toString()), lreq.getRequestBytes());

            LaudanumResponse lresp = new LaudanumResponse(callbacks, responseBytes);

            if (lresp.getCwd().length() > 0) {
                cwd = lresp.getCwd();
            }
            txtConsole.append(lresp.getStdout());
            txtConsole.append(lresp.getStderr());

        }
        history.add(command);
        historyPointer = history.size() - 1;
        txtConsole.append("\n");
        showPrompt();
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void showPrompt() {
        if (cwd.equals(".")) {
            txtConsole.append("laudanum> ");
        } else {
            txtConsole.append("laudanum[" + cwd + "]> ");
        }
        txtConsole.setCaretPosition(txtConsole.getText().length());
        commandStart = txtConsole.getCaretPosition();
        if (!txtConsole.hasFocus()) {
            txtConsole.grabFocus();
        }
    }


    @Override
    public Component getTabComponent() {
        return mainPanel;
    }

    @Override
    public String getTabTitle() {
        return "Laudanum";
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // do nothing
    }

    @Override
    public String getTabCaption() {
        return getTabTitle();
    }

    @Override
    public Component getUiComponent() {
        return getTabComponent();
    }

    private String getPrepend() {
        return "<none>".equals(cmboPrepend.getSelectedItem().toString()) ? "" : cmboPrepend.getSelectedItem().toString();
    }

    @Override
    public java.util.List<JMenuItem> createMenuItems(IContextMenuInvocation invocation) {
        IHttpRequestResponse[] messages = invocation.getSelectedMessages();
        if (messages != null && messages.length > 0) {
            callbacks.printOutput("Messages in array: " + messages.length);
            java.util.List<JMenuItem> list = new ArrayList<JMenuItem>();
            final IHttpService service = messages[0].getHttpService();
            final byte[] sentRequestBytes = messages[0].getRequest();
            JMenuItem menuItem = new JMenuItem("Send to Laudanum");
            menuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        requestTemplate = sentRequestBytes;
                        IRequestInfo info = callbacks.getHelpers().analyzeRequest(service, requestTemplate);
                        txtHostname.setText(service.getHost());
                        cmboProtocol.setSelectedItem(service.getProtocol());
                        txtResource.setText(info.getUrl().getFile());
                        txtPort.setText("" + info.getUrl().getPort());
                        lblRequestTemplate.setText(info.getUrl().toString());
                        chkUseRequestTemplate.setEnabled(true);
                        chkUseRequestTemplate.setSelected(true);
                        callbacks.printOutput("Laudanum received request template for " + info.getUrl().toString());
                        extender.selectConfigurableTab(LaudanumClient.this, true);
                    } catch (Exception e1) {
                        callbacks.printError(e1.getMessage());
                    }
                }
            });
            list.add(menuItem);
            return list;
        }

        return null;
    }
}
