# Fundraiser Tracker

An AWS Lambda application written in Quarkus uses DynamoDB as storage, exposes REST interface to get data 
and Slack command REST interface to manipulate data. Application supports currency conversion with rates obtained 
from Monobank API.

Additionally, application supports automatic synchronization of donations using Monobank and Privatbank sync by reacting to REST calls.

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/224eb30e7e5c4f38934ee9601e15237e)](https://www.codacy.com/gh/yuriytkach/fundraiser-tracker/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=yuriytkach/fundraiser-tracker&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://app.codacy.com/project/badge/Coverage/224eb30e7e5c4f38934ee9601e15237e)](https://www.codacy.com/gh/yuriytkach/fundraiser-tracker/dashboard?utm_source=github.com&utm_medium=referral&utm_content=yuriytkach/fundraiser-tracker&utm_campaign=Badge_Coverage)
![Build](https://github.com/yuriytkach/fundraiser-tracker/actions/workflows/gradle-no-native.yml/badge.svg)

## Supported Slack commands

If you add a [slash command](https://api.slack.com/interactivity/slash-commands) to Slack (for example `/fund`) then the following commands are supported by the application.

**Create new fund:**
```text
/fund create car eur 5000 /Banderomobil/ blue
```
Creates new fund with short name `car` (will be used to track donations) with goal of €5000, with full description `Banderomobil` and color `blue` (used in UI tracker)

```text
/fund create dron usd 7000
```
Creates a new fund with short name and description `dron` with goal of $7000 and default color `green` on UI

**Supported currencies:**
`UAH, USD, EUR, PLN, GBP, CHF`

**Update fund:**
```text
/fund update car open close curr:eur goal:5000 desc:/Banderomobil/ color:blue bank:account
```
Updates the attributes of fund with name `car` by specifying their names, colon and new values (see example above). 
All attributes are optional so that you can update only needed, but provider then in the above order.

- `open` or `close` perform either enabling or disabling of fund.
- `bank` can specify several accounts coma-separated.

**Updatable attributes:** currency(`curr`), goal(`goal`), description(`desc`), color(`color`), bank accounts(`bank`)

**List all funds:**
```text
/fund list
```
Displays all created funds with status (how much raised)

**Track donation:**
```text
/fund track car eur 500 Ivan 2022-05-12 14:15
/fund track car uah 500 Ivan 14:15             - track for today's date
/fund track car usd 500 Ivan                   - track for today's date and time
/fund track car usd 500                        - track for noname person
```
This will track donation for fund with short name `car`. 
- Any supported currency is allowed. Currency conversion takes place.
- Date and time is optional.
- Person name is optional. If not supplied, then `noname` is used.

**Delete fund:**
`Warning! This action cannot be undone! Use with caution!`
```text
/fund delete car
```
This will delete fund `car` with all recorded donations. _**Warning! - Cannot be undone!**_

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./gradlew build
```
It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./gradlew build -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./gradlew build -Dquarkus.package.type=native
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/code-with-quarkus-1.0.0-SNAPSHOT-runner`

## Running in AWS
You can manually create Lambda function in AWS from generated `function.zip`. Add the API Gateway for function's REST API.

You should also create DynamoDB `funds` table with indexes.

Additionally, you should create secret for Slack token and Privatbank token in AWS Secrets Manager.

If you want to sync Privatbank on a regular basis then you can create Event in the AWS EventBridge that will call Lambda.

## Related Guides

- REST Client Classic ([guide](https://quarkus.io/guides/rest-client)): Call REST services
- YAML Configuration ([guide](https://quarkus.io/guides/config-yaml)): Use YAML to configure your Quarkus application
- AWS Lambda Gateway REST API ([guide](https://quarkus.io/guides/amazon-lambda-http)): Build an API Gateway REST API with Lambda integration
- Amazon DynamoDB ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-amazon-services/dev/amazon-dynamodb.html)): Connect to Amazon DynamoDB datastore
- RESTEasy Classic ([guide](https://quarkus.io/guides/resteasy)): REST endpoint framework implementing Jakarta REST and more
- Amazon Secrets Manager ([guide](https://quarkiverse.github.io/quarkiverse-docs/quarkus-amazon-services/dev/amazon-secretsmanager.html)): Connect to Amazon Secrets Manager
