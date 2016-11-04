package CardHelper;

import org.apache.commons.mail.SimpleEmail;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by 41237 on 2016/10/22.
 */
public class SendMail
{
    private final String hostName;
    private final String hostPsw;
    private final String supplier = "smtp.163.com";
    private final String name = "一卡通助手";

    public SendMail(String name, String psw)
    {
        this.hostName = name;
        this.hostPsw = psw;
    }

    public void send(String targetAdr, String title, String content) throws Exception
    {
        SimpleEmail email = new SimpleEmail();
        email.setSSLOnConnect(true);
        email.setCharset("utf-8");
        email.setHostName(supplier);
        email.setAuthentication(hostName, hostPsw);
        email.setFrom(hostName, name);
        email.addTo(targetAdr);
        email.setSubject(title);
	    email.setMsg(content);
        email.send();
    }
}
