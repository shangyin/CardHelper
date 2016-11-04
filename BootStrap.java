package CardHelper;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by 41237 on 2016/11/3.
 */
public class BootStrap
{
    private final static String separator = System.lineSeparator();
    private static String ps;

    public static void main(String[] args) throws Exception
    {
        SAXReader saxReader = new SAXReader();
        Document doc = saxReader.read(new File(args[0]));
        Element root = doc.getRootElement();
        SendMail host = new SendMail(root.elementText("email"), root.elementText("password"));
        ps = root.elementText("ps");

        LocalTime from = LocalTime.now().withMinute(0).withSecond(0).withNano(0);
        LocalTime to = from.plusHours(1).minusMinutes(1);
        List<User> users = User.researchUsers(from, to);

        Executor executor = Executors.newFixedThreadPool(10);
        List<CompletableFuture<String>> recordsFutures = users.stream()
                .map(u -> CompletableFuture.supplyAsync(() -> send(host, u), executor))
                .collect(Collectors.toList());
        recordsFutures.stream()
                .map(CompletableFuture::join)
                .forEach(System.out::println);

    }

    private static String send(SendMail mail, User user)
    {
        try
        {
            XinXiMenHu xinXiMenHu = new XinXiMenHu(user.getName(), user.getPassword());
            CardCenter cardCenter = new CardCenter(xinXiMenHu);
            LocalDate date = LocalDate.now();
            if (LocalTime.now().getHour() < 21) {
                date = date.minusDays(1);
            }
            List<Record> records = cardCenter.getRecords(date.format(DateTimeFormatter.BASIC_ISO_DATE));
            mail.send(user.getEmail(), generateTitle(records), generateContent(records));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return  "finish user " + user;
    }

    private static String generateTitle(List<Record> records)
    {
        StringBuilder title = new StringBuilder();
        title.append("共消费：" + records.stream().mapToDouble(x -> x.getAmount().doubleValue()).sum());
        if (records.size() != 0)
        {
            title.append(" 余额：" + records.get(0).getTotal().doubleValue());
        }
        else
        {
            title.append(" 没有记录");
        }
        return title.toString();
    }

    private static String generateContent(List<Record> records)
    {
        StringBuilder sb = new StringBuilder();
        //具体消费条目
        String consume = records.stream()
                .filter(x -> x.getType() == Record.TYPE.CONSUME)
                .map( x -> x.getAmount() + " " + x.getPlace() + " " + x.getTime())
                .collect(Collectors.joining(separator));
        String other = records.stream()
                .filter(x -> x.getType() == Record.TYPE.UNKNOWN)
                .map(x -> x.getAmount() + " " + x.getsType() + " " + x.getPlace() + " " + x.getTime())
                .collect(Collectors.joining(separator));
        sb.append("消费：" + separator);
        sb.append(consume);
        sb.append(separator + separator);
        sb.append("其它交易：" + separator);
        sb.append(other);
        sb.append(separator + separator);
        sb.append(ps);
        return sb.toString();
    }
}
