# Shopping Chart App

## Description

ShoppingCartApp is a simulated customer shopping application.  A ShoppingCart web service exposes a REST api, using 
[akka http](https://doc.akka.io/docs/akka-http/current/), that allows users to browse the product catalog, add products 
to cart, query for cart status, and ultimately to commit the cart.  Shopping carts are distributed between the nodes of
the [akka cluster](https://doc.akka.io/docs/akka/current/cluster-usage.html) using 
[akka cluster sharding](https://doc.akka.io/docs/akka/current/cluster-sharding.html). 
[Cinnamon](https://developer.lightbend.com/docs/cinnamon/current/getting-started/start.html) is used to report cluster 
telemetry as well as custom business metrics.  Finally, Enterprise Suite Console is used to monitor the application.  You 
can run the whole setup using [minikube](https://kubernetes.io/docs/setup/minikube/)

# Installation Instructions

## Prerequisites

- [Docker is installed](https://www.docker.com/community-edition)
- [minikube running and ES-Console installed](https://docs.google.com/document/d/10A3qyMr04kBPpjQSLH6iJsu9yICL7T_fc0u6HRUSIdc/view)
- [you have valid Bintray credentials](https://developer.lightbend.com/docs/reactive-platform/2.0/setup/setup-sbt.html#bintray-credentials) 
set up on your dev box

## Building and Deploy

set docker env for minikube

```
eval $( minikube docker-env )
```

build the docker image

```
sbt shoppingcartapp/docker:publishLocal
```  

deploy the image onto minikube

```
kubectl apply -f shoppingcartapp/shoppingcartapp.yaml
```

## Installing Custom Metrics Dashboard

ShoppingCartApp comes with a custom Grafana dashboard to display business metrics via Cinnamon. To install the the dashboard, 

1. copy the contents of [shoppingcartapp/grafana.json](grafana.json) to your clipboard
1. open your browser to the ES-Console
1. click on the Grafana link in the upper left corner
1. click on the 'Import Dashboard' link in Grafana from the left navigation bar.  Paste the contents of your clipboard 
and click 'Load'
