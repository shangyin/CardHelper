package CardHelper;


import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by A1 on 2016/11/15.
 */
public class ContentUtil {
    final String type;
    final String ps;
    final LocalDate from;
    final LocalDate to;
    final String fromStr;
    final String toStr;
    private final static String separator = System.lineSeparator();

    Map<String, Function<List<Record>, String>> contentMap = new HashMap<>();
    Map<String, Function<List<Record>, String>> titleMap = new HashMap<>();
    Function<List<Record>, String> content;
    Function<List<Record>, String> title;

    public ContentUtil(String type, String ps) {
        LocalDate tmp = LocalDate.now().minusDays(1);
        switch (type) {
            case "daily":
                from = LocalTime.now().getHour() >= 21 ? LocalDate.now() : LocalDate.now().minusDays(1);
                to = from;
                break;
            case "weekly":
                from = tmp.minusDays(tmp.getDayOfWeek().getValue() - 1);
                to = from.plusDays(6);
                break;
            case "monthly":
                from = tmp.minusDays(tmp.getDayOfMonth() - 1);
                to = from.plusMonths(1).minusDays(1);
                break;
            default:
                from = LocalDate.now();
                to =  LocalDate.now();
        }
        this.ps = ps;
        this.type = type;
        this.fromStr = from.format(DateTimeFormatter.BASIC_ISO_DATE);
        this.toStr = to.format(DateTimeFormatter.BASIC_ISO_DATE);


        contentMap.put("daily", this::dayContent);
        contentMap.put("weekly", this::weekContent);
        contentMap.put("monthly", this::monthContent);
        titleMap.put("daily", this::dayTitle);
        titleMap.put("weekly", this::weekTitle);
        titleMap.put("monthly", this::monthTitle);
        title = titleMap.get(type);
        content = contentMap.get(type);
    }

    public String getContent(List<Record> records) {
        return content.apply(records);
    }

    public String getTitle(List<Record> records) {
        return title.apply(records);
    }


    private String dayTitle(List<Record> records)
    {
        StringBuilder title = new StringBuilder();
        title.append("共消费：" + records.stream().mapToDouble(x -> x.getAmount().doubleValue()).sum());
        if (records.size() != 0) {
            title.append(" 余额：" + records.get(0).getTotal().doubleValue());
        } else {
            title.append(" 没有记录");
        }
        return title.toString();
    }

    private String dayContent(List<Record> records)
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

    private String weekTitle(List<Record> records) {
        return "本周消费小计";
    }

    private String weekContent(List<Record> records) {
        double con = records.stream()
                .filter(x -> x.getType() == Record.TYPE.CONSUME)
                .mapToDouble( x -> x.getAmount().doubleValue())
                .sum();
        double charge = records.stream()
                .filter(x -> x.getType() == Record.TYPE.UNKNOWN)
                .mapToDouble( x -> x.getAmount().doubleValue())
                .sum();

        StringBuilder sb = new StringBuilder();
        sb.append("日期：");
        sb.append(from);
        sb.append(" - ");
        sb.append(to);
        sb.append(separator);
        sb.append("消费");
        sb.append(Math.abs(con));
        sb.append("元，充值");
        sb.append(charge);
        sb.append(separator);
        if (records.size() != 0) {
            sb.append("余额");
            sb.append(records.get(0).getTotal());
            sb.append("元，刷卡");
            sb.append(records.size());
            sb.append("次");
        }
        sb.append(separator + separator);
        sb.append(ps);
        return sb.toString();
    }

    private String monthTitle(List<Record> records) {
        return "本月消费小计";
    }

    private String monthContent(List<Record> records) {
        return weekContent(records);
    }
}
