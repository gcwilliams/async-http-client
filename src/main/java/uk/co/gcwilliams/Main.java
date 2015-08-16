package uk.co.gcwilliams;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import uk.co.gcwilliams.http.AsyncHttpClient;
import uk.co.gcwilliams.http.AsyncHttpMessage;
import uk.co.gcwilliams.http.NettyAsyncHttpClient;
import uk.co.gcwilliams.http.tasks.Task;

import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.Math.random;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static uk.co.gcwilliams.http.tasks.Tasks.all;

/**
 * The test main
 *
 * Created by GWilliams on 15/08/2015.
 */
public class Main {

    public static void main(String... args) throws Exception {

//        Task<String> task = Task.of("this is a ").map(s -> s + "value in a task");

//
//        Task<Double> getFirstValue = resolveLater(24);
//        Task<Double> getSecondValue = resolveLater(19);
//        Task<Double> getThirdValue = resolveLater(13);
//
//
//        Task<List<Double>> all = join(getFirstValue, getSecondValue, getThirdValue).map(v -> {
//            System.out.println(v);
//            return v;
//        });
//
//        // run the task
//        all.fork(v -> System.out.println("Success!"), System.out::println);


        EventLoopGroup loop = new NioEventLoopGroup(1);
        AsyncHttpClient client = new NettyAsyncHttpClient(loop);

//        Function<String, Task<String>> getTitle  = s -> client.get(s)
//            .map(Jsoup::parse)
//            .map(d -> d.head().getElementsByTag("title").first())
//            .map(e -> ofNullable(e).map(Element::text).orElse("Not Found"));
////            .map(t -> {
////                throw new NullPointerException();
////            });
//
//        Task<String> news = getTitle.apply("http://www.bbc.co.uk/news");
//        Task<String> sport = getTitle.apply("http://www.bbc.co.uk/sport/0/");
//        Task<String> weather = getTitle.apply("http://www.bbc.co.uk/weather/");
//        Task<String> tv = getTitle.apply("http://www.bbc.co.uk/tv");
//        Task<String> iPlayer = getTitle.apply("http://www.bbc.co.uk/iplayer");
//
//        for (int i = 0; i < 100; i++) {
//            final int idx = i;
//            all(news, sport, weather, tv, iPlayer).fork(values -> {
//                System.out.println(values);
//                System.out.println("completed " + idx);
//            }, System.out::println);
//        }


        Function<AsyncHttpMessage, String> convertBody = m -> new String(m.getContent(), Charset.forName("utf-8"));
        Function<String, String> truncateBody = b -> b.substring(0, 200);

        // combine with the client
        Function<String, Task<String>> hotelTask = url -> client.get(url).map(convertBody).map(truncateBody);



        String[] hotels = new String[] { "H2156","H0713","H1007","H2233","H4727","H4722","H0059","H0578","H0624","H4683","H2959","H1134","H4703","H0903","H2101","H1879","H2264","H0388","H0009","H2909","HB119735","H2966","HB120535","HB111313","HB120794","HB124359","H2365","HB118143","HB122264","HB120648","HB130781","HB125718","HB130666","HB130678","HB130700","HB12828","HB130837","HB128813","HB13105","HB12936","HB129727","HB104399","HB108106","HB112880","HB130755","HB124760","HB106395","HB107893","HB122579","HB130577","HB130351","HB125530","HB139543","HB130356","HB139360","HB144857","HB139562","HB132011","HB138824","HB136190","HB16604","HB16762","HB134980","HB159452","HB132667","HB132465","HB131905","HB141550","HB167568","HB14836","HB151016","HB159318","HB147717","HB156249","HB16159","HB161077","HB24739","HB251885","HB252578","HB251880","HB183689","HB188711","HB168441","HB148037","HB183898","HB197969","HB192267","HB252376","HB17227","HB7578","HB90076","HB91000","HB9125","HB65214","HB7535","HB3038","HB65651","HB64499","HB7573","HB69622"};

        Task<List<String>> task = all(stream(hotels)
            .map(s -> "http://vsolpublic-test1.webdev.vholsinternal.co.uk/locations-public-api/hotels/" + s)
            .map(hotelTask)
            .collect(toList()));

        Consumer<List<String>> consumer = s -> {
            System.out.println(s);
            loop.shutdownGracefully();
        };

        task.fork(consumer, System.out::println);


//        List<String> values = get(task, 10000);
//
//        System.out.println(values);

//        loop.shutdownGracefully();
    }

    private static Task<Double> resolveLater(int seed) {
        return new Task<>((resolve, reject) -> {
            new Thread(() -> {
                try {
                    System.out.println("starting");
                    Thread.sleep((int)(random() * 5000));
                    System.out.println("resolved");
                    resolve.accept(new SecureRandom(new byte[]{(byte) seed}).nextDouble());
                } catch (InterruptedException e) {
                    reject.accept(e);
                }
            }).start();
        });
    }
}
