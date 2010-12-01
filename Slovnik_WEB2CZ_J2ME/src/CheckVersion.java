import javax.microedition.lcdui.*;
import javax.microedition.io.*;
import java.io.*;

class CheckVersion extends Thread {

	private StringItem stringItem = null;
	private String VERSION = "";
	private String URL = "";

	public CheckVersion(StringItem stringItem, String VERSION, String URL) {
		this.stringItem = stringItem;
		this.VERSION = VERSION;
		this.URL = URL;
	}

	public void run() {
		try {
			checkVersion(stringItem, VERSION, URL);
		} catch (Exception e) {}
	}

	private void checkVersion(StringItem stringItem, String VERSION, String URL) {
		String netVersion = checkNetVersion(URL);

		stringItem.setLabel("Va�e verze slovn�ku " + VERSION + "\n");

		if((netVersion == "") || (netVersion.compareTo("0.0.0") == 0)) stringItem.setText("Aktu�ln� verzi slovn�ku se bohu�el nepoda�ilo zjistit. V�ce informac� naleznete na webov� adrese " + URL + "/mobil.");
		else if(netVersion.compareTo(VERSION) == 0) stringItem.setText("M�te aktu�ln� verzi slovn�ku " + VERSION +  ". V�ce informac� naleznete na webov� adrese " + URL + "/mobil.");
		else stringItem.setText("M�te nainstalov�nu star�� verzi slovn�ku " + VERSION + ". Aktu�ln� verzi " + netVersion + " si m��ete st�hnout na webov� adrese " + URL + "/mobil nebo na wapov� str�nce " + URL + "/wap.");
	}

	public String checkNetVersion(String URL) {
		try {
			HttpConnection connection = null;
			InputStream input = null;

			try {
				connection = (HttpConnection) Connector.open(URL + "/j2me/version.php");
				connection.setRequestMethod(HttpConnection.GET);

				input = connection.openInputStream();

				String version = null;

				int len = (int) connection.getLength();
				if(len > 0) {
					byte[] data = new byte[len];
					int actual = input.read(data);
					version = new String(data);
				} else {
					StringBuffer buffer = new StringBuffer();
					int ch;
					while((ch = input.read()) != -1) buffer.append((char) ch);
					version = buffer.toString();
				}

				return version.trim();
			} catch(Exception e) {
				return "";
			} finally {
				if(input != null) input.close();
				if(connection != null) connection.close();
			}
		} catch(Exception e) {
			return "";
		}
	}
}