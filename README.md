
# Kerberos (GSSAPI) authentication for Java Grpc

This project contains a minimal implementation of a Kerberos based authentication for Grpc server.

The main workflow is based the authentication of the Apache Hadoop RPC but it's simplified. The biggest difference here is that the whole mechanism negotiation is missing from here as this plugin always use Kerberos.

The implementation can be used only for an initial authentication call, but the initial call can return with any token which can be used in the subsequent business calls to be authorized.

## Usage

The server side provides a new service which can be used to negotiate based on your Kerberos credentials:

```java
Server server = ServerBuilder
   .forPort(1234)
   .addService(new NegotiationService(authorizationListener))
   .addService(/*... your service ... */)
   .build();
```

The authorizationListener is a simple interface which will be caled in case of a successful authentication.

```java
public interface AuthenticationListener {

  byte[] authenticated(String principal);
}
```

The return value of `authenticated` method can be any payload. With the help of this payload the other services can check if the caller already authenticatede with the help of Kerberos. See the `TokenService` as an example. (Just an example, for example expiration is not handled.)

On the client side, you can use the `NegotationClient`:

```java
ManagedChannel channel =
          ManagedChannelBuilder.forAddress("localhost", 1234).usePlaintext().build();

NegotiationClient client = new NegotiationClient(channel);

byte[] token = negotiationClient.negotiate(); 

```

In case of a successful authentication the token contains the output from the `AuthenticationListener`.

Both client and server can use the local Kerberos cache (managed by `kinit`/`kdestroy` or any keytab + principal). For example the client has the following constructors:

```java
//use the keytab + principal
NegotiationClient client = new NegotiationClient(channel, keytab, principal);

//use local cache
NegotiationClient client = new NegotiationClient(channel);
```

Implement your own keytab renewal logic in case of using the local cache.

*Validation of the token*:

There are two main approach to check the token on the server side.

 1. You can put it to the headers (See `TokenClientInterceptor` and `TokenFilterInterceptor` as an example)
 2. You can put it to you message itself (preferred if you need the information of the current principal)

## Example

`examples` project contains a simple GRPC client and server which can be used for testing:

```shell
mvn clean install

./examples/target/integration/server.sh --keytab /tmp/server.keytab --principal server/server@EXAMPLE.COM

./examples/target/integration/client.sh
```

(Note: both pf them can be started with/without keytabs).


