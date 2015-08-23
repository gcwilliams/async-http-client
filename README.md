### Async HTTP Client

An async http client utilizing [Netty](http://netty.io/) and a functional approach inspired by
the awesome DrBoolean and his [PointFree-Fantasy](https://github.com/DrBoolean/pointfree-fantasy) project and
the [Folktale Task](https://github.com/folktale/data.task) library

#### Simple Usage

    // create a new client
    EventLoopGroup loop = new NioEventLoopGroup(1);
    AsyncHttpClient client = new NettyAsyncHttpClient(loop);

    // create a task
    Task<AsyncHttpMessage, Exception> task = client.get("http://www.google.co.uk");

    // fork the task
    task.fork(m -> {

        // log the status
        System.out.println(m.getStatusCode());

        // shutdown the client
        loop.shutdownGracefully();

    }, System.out::println);

#### Composing Operations

As inspired by `PointFree-Fantasy`, operations can be composed together

    // create a new client
    EventLoopGroup loop = new NioEventLoopGroup(1);
    AsyncHttpClient client = new NettyAsyncHttpClient(loop);

    // create operation
    Function<AsyncHttpMessage, String> decodeBody = message -> new String(message.getContent(), Charset.forName("utf-8"));

    Function<String, Document> parseHtml = Jsoup::parse;

    Function<Document, String> findTitle = doc -> doc.head().select("title").text();

    Function<AsyncHttpMessage, String> getPageTitle = compose(findTitle, parseHtml, decodeBody);

    Function<String, Task<AsyncHttpMessage, Exception>> download = client::get;

    Function<String, Task<String, Exception>> operation = compose(fmap(getPageTitle), download);

    // create the task
    Task<String, Exception> task = operation.apply("http://www.bbc.co.uk");

    // run the task
    task.fork(title -> {

        // log the status
        System.out.println(title);

        // shutdown the client
        loop.shutdownGracefully();

    }, System.out::println);

#### Tasks

The tasks class provides utilities for working with tasks.

##### Creating Tasks

    // String -> Task<String, Exception>
    Task<String, Exception> task = Tasks.of("");

    // String -> Task<String, Exception>
    Task<String, Exception> task = Tasks.rejected(new RuntimeException());

##### Combining Tasks

Create a task that resolves when all of the provided tasks are resolved

    // Task<String, Exception>... -> Task<List<String>, List<Exception>>
    Task<List<String>, List<Exception>> task = Tasks.all(Tasks.of(""), Tasks.of(""));

Create a task that resolves when any of the provided tasks are resolved

    // Task<String, Exception>... -> Task<String, Exception>
    Task<String, Exception> task = Tasks.any(Tasks.of(""), Tasks.of(""));

##### Waiting

You _can_ wait on a task to complete, this operation will throw an exception if that task fails or the timeout is exceeded

    // Task<String> -> String
    String result = Tasks.get(Tasks.of(""), 1000);

