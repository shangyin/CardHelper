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
    public static final String separator = System.lineSeparator();
    private final String hostName;
    private final String hostPsw;

    public SendMail(String name, String psw)
    {
        this.hostName = name;
        this.hostPsw = psw;
    }

    public static void main(String[] args) throws Exception
    {
        SAXReader saxReader = new SAXReader();
        Document doc = saxReader.read(new File(args[0]));
        Element root = doc.getRootElement();
        SendMail host = new SendMail(root.elementText("email"), root.elementText("password"));

        int currentHour = LocalTime.now().getHour();
        List<User> users = User.parseUsers(root.elementText("users"));
        List<User> targetUsers = users.stream()
                                        .filter(x -> x.getLocalTime().getHour() == currentHour)
                                        .collect(Collectors.toList());
        for (User u : targetUsers) {
            new Thread(new Handler(u, host)).start();
        }
    }

    private static class Handler implements Runnable
    {
        private User user;
        private SendMail mail;
        private Handler(User user, SendMail mail)
        {
            this.user = user;
            this.mail = mail;
        }

        public void run()
        {
            try
            {
                //登录信息门户
                XinXiMenHu cardInfo = new XinXiMenHu(user.getName(), user.getPassword());
                CardCenter cardCenter = new CardCenter(cardInfo);
                //查询一卡通日期，20点以前查询昨日
                LocalDate target;
                if (user.getLocalTime().getHour() < 20) {
                    target = LocalDate.now().minusDays(1);
                } else {
                    target = LocalDate.now();
                }
                String date = target.format(DateTimeFormatter.BASIC_ISO_DATE);
                List<Record> records = cardCenter.getRecords(date, date);
                cardInfo.close();
                //发送邮件
                String content = generateContent(records);
                mail.send(user.getEmail(), "昨日消费", content);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private static String generateContent(List<Record> records)
    {
        StringBuilder sb = new StringBuilder();
        //消费概括
        sb.append("共消费：" + Record.addUpConsume(records) + separator);
        if (records.size() == 0) {
            sb.append("昨天没有使用饭卡！");
        } else {
            sb.append("当前余额为：" + records.get(0).getTotal());
        }
        sb.append(separator);
        //具体消费条目
        String consume = records.stream()
                .filter(x -> x.getType() == Record.TYPE.CONSUME)
                .map( x -> x.getAmount() + " " + x.getPlace() + " " + x.getTime())
                .collect(Collectors.joining(separator));
        String other = records.stream()
                .filter(x -> x.getType() == Record.TYPE.UNKNOWN)
                .map(x -> x.getAmount() + " " + x.getPlace() + " " + x.getTime())
                .collect(Collectors.joining(separator));
        sb.append("消费：" + separator);
        sb.append(consume);
        sb.append("其它交易：" + separator);
        sb.append(other);
        sb.append(separator + "信息来自信息门户一卡通系统，记录存在延迟，仅供参考");
        return sb.toString();
    }

    public void send(String targetAdr, String title, String content) throws Exception
    {
        SimpleEmail email = new SimpleEmail();
        email.setSSLOnConnect(true);
        email.setCharset("utf-8");
        email.setHostName("smtp.163.com");
        email.setAuthentication(hostName, hostPsw);
        email.setFrom(hostName);
        email.addTo(targetAdr);
        email.setSubject(title);
	    email.setMsg(content);
        email.send();
    }
}
