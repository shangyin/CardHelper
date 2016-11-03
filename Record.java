package CardHelper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Created by 41237 on 2016/10/21.
 */
public class Record
{
    public enum STATE
    {
        NORMAL, UNKNOWN;
    }
    public enum TYPE
    {
        CONSUME, UNKNOWN;
    }


    private LocalDate date;
    private LocalTime time;
    private BigDecimal amount;
    private BigDecimal total;
    private int count;
    private STATE state;
    private String sType;

    public String getPlace()
    {
        return place;
    }

    public TYPE getType()
    {
        return type;
    }

    private String place;
    private TYPE type;

    public LocalDate getDate()
    {
        return date;
    }

    public LocalTime getTime()
    {
        return time;
    }

    public BigDecimal getAmount()
    {
        return amount;
    }

    public BigDecimal getTotal()
    {
        return total;
    }

    public int getCount()
    {
        return count;
    }

    public STATE getState()
    {
        return state;
    }

    public String getsType()
    {
        return sType;
    }

    public String toString()
    {
        return amount + "\t" + total + "\t" + date + "\t" + time;
    }


    public Record(RecordBuilder builder)
    {
        this.date = builder.date;
        this.time = builder.time;
        this.amount = builder.amount;
        this.total = builder.total;
        this.count = builder.count;
        this.state = builder.state;
        this.place = builder.place;
        this.type = builder.type;
        this.sType = builder.sType;
    }

    public static double addUpConsume(List<Record> records)
    {
        return records.stream()
                .filter(x -> x.type == TYPE.CONSUME)
                .mapToDouble(x -> x.amount.doubleValue())
                .sum();
    }

    public static class RecordBuilder
    {
        private LocalDate date;
        private LocalTime time;
        private BigDecimal amount;
        private BigDecimal total;
        private int count = 0;
        private STATE state = STATE.NORMAL;
        private String place = "";
        private TYPE type = TYPE.UNKNOWN;
        private String sType = "";

        public RecordBuilder(BigDecimal amount, BigDecimal total, LocalDate date, LocalTime time)
        {
            this.amount = amount;
            this.total = total;
            this.date = date;
            this.time = time;
        }

        public RecordBuilder setState(String state)
        {
            switch (state)
            {
                case "正常":
                    this.state = STATE.NORMAL;
                    break;
                default:
                    this.state = STATE.UNKNOWN;
            }
            return this;
        }

        public RecordBuilder setCount(int count)
        {
            this.count = count;
            return this;
        }

        public RecordBuilder setPlace(String place)
        {
            this.place = place;
            return this;
        }

        public RecordBuilder setType(String type)
        {
            switch (type)
            {
                case "持卡人消费":
                    this.sType = type;
                    this.type = TYPE.CONSUME;
                    break;
                default:
                    this.sType = type;
                    this.type = TYPE.UNKNOWN;
            }
            return this;
        }
    }
}
