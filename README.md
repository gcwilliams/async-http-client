# Async HTTP Client

[![Java CI with Maven](https://github.com/gcwilliams/async-http-client/actions/workflows/maven.yml/badge.svg)](https://github.com/gcwilliams/async-http-client/actions/workflows/maven.yml)

- Async HTTP requests
- HTTP / HTTPS
- Connection Pooling
- SNI

### Usage

### Requests

Requests can be creating using the `AsyncHttpRequest` class and builder

    AsyncHttpRequest request = AsyncHttpRequest.builder()
        .withMethod(AsyncHttpRequest.HttpMethod.POST)
        .withURI(URI.create("https://api.somewhere.com"))
        .withHeader("Authorization", "Basic YWRtaW46YWRtaW4xMjM=")
        .build();

or using the shorter methods

    AsyncHttpRequest request = AsyncHttpRequest.get(URI.create("https://api.somewhere.com")).build();

### Listeners

Listeners can be used to implement logging, set default headers on requests, handle ThreadLocal variables and more.

Some common listeners are in the package `uk.co.gcwilliams.async.http.listeners`

    // logging
    LoggingListener.INSTANCE

    // set default headers
    new DefaultHeadersListener("Authorization", "Basic YWRtaW46YWRtaW4xMjM=");

### HTTP Client

The HTTP client can be created using the builder on the `NettyAsyncHttpClient` class. Various configuration
options are available on the builder.

    AsyncHttpClient http = NettyAsyncHttpClient.builder().build();

Once you have built the HTTP client you can prepare request to be sent.

    Task<AsyncHttpResponse> response = http.prepare(request);

And to actually send the request, you need to fork the task and provide 2 consumers, one for the response and one
for any exception that might occur.

    response.fork(
        response -> {
            // handle response
        },
        exception -> {
            // handle exception
        });

## Tasks

The abstraction on which asynchronous computations can be built upon.

### Creating Tasks

    Task<String> action = Task.of("Some Value");

    Task<String> action = Task.of(new IllegalStateException("Some Value"));

    Task<String> action = Task.of((resolve, reject) -> {

        // some operation

        // THEN
        resolve.accept("Some Value");
        // OR
        reject.accept(new IllegalStateException("Some Value"));
    });

### Working With Tasks

#### Map

Mapping over the value of a task, just like `java.util.stream.Stream#map`

    Task<String> action = Task.of("1");
    Task<Integer> mappped = action.map(value -> Integer.parse(value));

#### FlatMap

Mapping over the value of a task and returning another task, just like `java.util.stream.Stream#flatMap`

    Task<String> action = Task.of("1");
    Task<Integer> flatMappped = action.flatMap(value -> Task.of(Integer.parse(value)));

#### Lists of Tasks

Converting a `List<Task<...>>` to `Task<List<...>>` 

    Task<String> givenName = Task.of("Homer");
    Task<String> surname = Task.of("Simpson");
    List<Task<String>> names = List.of(givenName, surname);

    Task<List<String>> action = Tasks.traverse(names);
    // or in parallel
    Task<List<String>> action = Tasks.traverseP(names);

#### Applying Multiple Tasks

Multiple tasks with different types

    Task<String> one = Task.of("Homer");
    Task<Integer> two = Task.of(1);

    Task<String> three = Tasks.apply(one, two, (homer, one) -> Task.of(String.format("%s is number %s", homer, one)));
    // homer is number 1

#### Get

Useful in Unit Tests

    // arrange
    Task<String> action = Task.of("Homer");

    // act
    String homer = Tasks.get(action, Duration.ofMinutes(1));

    // assert
    assertThat(homer, equalTo("Homer"));

### Completion Stage

Tasks are simple and work great for the HTTP client, however, you might want to use `CompletionStage<T>` or `CompletableFuture<T>`
in the rest of your code base. You can use the CompletionStages utility class to convert a task to a `CompletionStage<T>`.

    Task<String> task = Task.of("Homer");

    CompletionStage<String> completionStage = CompletionStages.toCompletionStage(task);

    String homer = completionStage.toCompletableFuture().get(30, TimeUnit.SECONDS)
