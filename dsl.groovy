job("devops_task6_job1"){
  description("Pull the data from github repo automatically when some developers push code to github")
  scm{
    github("sushantb1508/task3_devops_","master")
  }
  triggers {
    scm("* * * * *")
  }
  steps{
    shell('''if ls / | grep task6_devops
then
sudo cp -rf * /task6_devops
else
sudo mkdir /task6_devops
sudo cp -rf * /task6_devops
fi  
''')
  }
}





job("devops_task6_job2"){
  description("By looking at the code it will launch the deployment of respective webserver and the deployment will launch webserver, create PVC and expose the deployment")

  triggers {
    upstream("devops_task6_job1", "SUCCESS")
  }
  steps{
    shell('''data=$(sudo ls /task6_devops)
if sudo ls /task6_devops/ | grep php
then
if sudo kubectl get deploy/webserver
then
echo "Already running"
POD=$(sudo kubectl get pod -l app=webserver -o jsonpath="{.items[0].metadata.name}")
for file in $data
do
sudo kubectl cp /task6_devops/$file $POD:/var/www/html/
done
else
sudo kubectl create -f /task6_devops/deploy.yml
POD=$(sudo kubectl get pod -l app=webserver -o jsonpath="{.items[0].metadata.name}")
sleep 30
for file in $data
do
sudo kubectl cp /task6_devops/$file $POD:/var/www/html/
done
fi
else
echo "No server found"
exit 1
fi
''')
  }
}




job("devops_task6_job3"){
    description("Testing Application")
	triggers{
		upstream('devops_task6_job2' , 'SUCCESS')
	}
	steps{
		shell('''status=$(curl -o /dev/null  -s  -w "%{http_code}"  http://192.168.99.101:30100/index.php)
if [ $status == 200 ]
then
exit 1
else
sudo python3 task6_devops/mail.py
fi
''')
	}

}





job("devops_task6_job4 "){
  description("This Job is created for monitoring of the container and to launch another if the existing fails.")

triggers {
    upstream("devops_task6_job3", "SUCCESS")
  }
  triggers {
    scm("* * * * *")
  }
  steps{
    shell('''if sudo kubectl get deployment | grep webserver
then
exit 0
else
sudo kubectl create -f /task6_devops/deploy.yml
sleep 10
fi
if sudo kubectl get pods | grep running
then
exit 0
else
echo "Pod is not running"
fi
''')
}
}



buildPipelineView("Pipeline view") {
  filterBuildQueue(true)
  filterExecutors(false)
  title("DSL pipeline")
  displayedBuilds(1)
  selectedJob("devops_task6_job1")
  alwaysAllowManualTrigger(true)
  showPipelineParameters(true)
  refreshFrequency(60)
}
