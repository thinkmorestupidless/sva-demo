
# SVA Demo

## Setting up Kubernetes

You'll need to get minikube running

```bash
minikube start --cpus 4 --memory 8192
```

Once that's running you'll need to configure Kubernetes to get Tiller running so helm charts can be deployed (Enterprise Suite Console is deployed via helm)

```bash
kubectl create serviceaccount --namespace kube-system tiller
kubectl create clusterrolebinding tiller-cluster-admin --clusterrole=cluster-admin --serviceaccount=kube-system:tiller
helm init --service-account tiller

kubectl create rolebinding default-view --clusterrole=view --serviceaccount=default:default --namespace=default
```

You'll also need helm installed locally

With MacOS this is easy with homebrew

```bash
brew install helm
```

Not sure how this is achieved on Windows or Linux, i'll try to find out for you, if necessary.

Now Kubernetes is ready we can view the [Kubernetes dashboard](http://192.168.99.100:30000/#!/overview?namespace=_all)

## Deploy the Enterprise Suite Console

```bash
./install-es.sh
```

This will install all the dependencies (and use the credentials that you set up earlier).

Once this is running you'll be able to see the workloads being deployed in the Kubernetes dashboard.

Once that's running (and everything's green) you can view the [Enterprise Suite Console](http://192.168.99.100:30080/)

## Build and Containerise the sample application

Firstly, make sure we're pointing at Minikube's docker registry

```bash
eval $(minikube docker-env)
```

Now, build and containerise and publish the container to Minikube's docker registry

```bash
sbt shoppingcartapp/docker:publishLocal
```

## Orchestrate the docker image

Firstly you'll need to install the `rp` tool

```bash
rp generate-kubernetes-resources --generate-all 'shoppingcartapp:0.1' --pod-controller-replicas 3 | kubectl apply -f -
```

Now, keep an eye on the Enterprise Suite Console for the cluster coming available.

At that point you can click on the `grafana` icon in the top left and that will open up the dashboards and you can see all the actor-level metrics coming through.

## Install the custom dashboard

In Grafana import the dashboard at `shoppingcartapp/grafana.json`

* Click the '+' on the left hand side of Grafana's UI
* Select 'Import'
* Paste the content of `shoppingcartapp/grafana.json` into the textfield
* Select 'Load' and then 'Import'