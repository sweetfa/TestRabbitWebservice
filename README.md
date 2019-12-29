# Web Service to Allow Web Service interface to RabbitMQ
This module is designed to provide a simple web interface to allow a test client, such as Postman, or SoapUI to send messages to an RabbitMQ instance.

It is a spring boot application.

___

## To run

``` mvn springboot:run ```

---

## Usage

This utility is invoked by a HTTP POST request with the http request paramaters specifing the queue names to send the message to.
Place a Json payload that is to be sent, within the HTTP Post request

### Sending a request and wait for a response
http://localhost:8086/rabbit/send?writeQueueName=yourRequestQueue&receiveQueueName=yourResponseQueue

### Sending without expecting a response
http://localhost:8086/rabbit/write?queueName=yourqueue

### Monitoring and logging all responses on a queue
http://localhost:8086/rabbit/monitor?queueName=yourqueue