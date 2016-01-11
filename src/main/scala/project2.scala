import akka.actor.{ Actor, ActorRef, Props, ActorSystem }
import akka.actor.actorRef2Scala
import akka.dispatch.ExecutionContexts.global
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration.DurationInt
import scala.io.Source.fromFile
import java.security.MessageDigest
import akka.routing.RoundRobinRouter
import akka.util.Timeout
import scala.concurrent.duration._
import akka.pattern.ask
import akka.dispatch.ExecutionContexts._
import scala.math._
import scala.util.Random
import akka.actor._ 
import scala.collection.mutable.ArrayBuffer
import scala.util.Random
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import scala.language.postfixOps
import scala.collection.immutable
 


case class CreateTopologyOfNodes()
case object ConvergenceAchieved
class networkMaster(algoSelected: String,totalNodesEntered: Double,topologyOfNetwork:String) extends Actor {
  //private var tries = 0
  private var countOfWorker = 0
  var startTime: Long = 0
  private var runningNodesCount: Int = 0
  var totalCalculatedNodes : Double = totalNodesEntered
  //private var actorcount = totalNodes
  def calculateTotalNodes(a: Double): Double = {
      if(topologyOfNetwork == "line" || topologyOfNetwork == "full"){
        return totalNodesEntered
      }else{
        return pow(ceil(cbrt(totalNodesEntered)) , 3)
      }
  }

  def instantiateAllParticipatingNodes(totalCalculatedNodes:Int) : ArrayBuffer[ActorRef] = {
      var participatingNodes = new ArrayBuffer[ActorRef]()
      for (i <- 0 until totalCalculatedNodes) {
      participatingNodes += context.actorOf(Props[WorkerNode], name = "participatingNodes:" + i) 
      participatingNodes(i) ! ResetSum(i.toDouble)
      }
      return participatingNodes
  }  

  def receive = {
    
    case msg : String => {
        println(msg)
     }   
        
    case CreateTopologyOfNodes() => {
          //find the appropriate totalNodes
          startTime = System.currentTimeMillis()
          totalCalculatedNodes = ceil(calculateTotalNodes(totalNodesEntered)).toDouble;
          println(totalCalculatedNodes);
          //create the workers for it.
          var participatingNodes = instantiateAllParticipatingNodes(totalCalculatedNodes.toInt)
          var randomNeighborToStart = Random.nextInt(totalCalculatedNodes.toInt)
          println(randomNeighborToStart)
          var sumNew = randomNeighborToStart
          var weightNew = 1
          //totalCalculatedNodes = totalCalculatedNodes.toInt
          if(algoSelected == "pushsum"){
          participatingNodes(randomNeighborToStart) ! StartPushSum(totalCalculatedNodes.toInt,topologyOfNetwork,sumNew,weightNew,1,participatingNodes)
          }else{
          participatingNodes(randomNeighborToStart) ! StartGossip(totalCalculatedNodes.toInt,topologyOfNetwork,1,participatingNodes)
          }

          
      }

     case ConvergenceAchieved => {
          //runningNodesCount = runningNodesCount + 1
          //if(runningNodesCount == totalCalculatedNodes) {    
          println("TimeTotal: " + (System.currentTimeMillis() - startTime) + " milliseconds")  
          context.system.shutdown()
          //println("Number of Nodes: " + totalNodes)
          
          //}
     } 

    case _ => println("Message not recognized!")
  }
}
 
case class StartGossip(totalCalculatedNodes:Int,topologyOfNetwork:String,invokedFirstTime:Int,actorNode:ArrayBuffer[ActorRef])
case class StartPushSum(totalCalculatedNodes:Int,topologyOfNetwork:String,sumNew: Double, weightNew: Double,invokedFirstTime:Int,actorNode:ArrayBuffer[ActorRef])
case class ResetSum(s: Double)
class WorkerNode extends Actor {
import context._

  var receivedNeighborArray = new ArrayBuffer[Int]()
  var master: ActorRef = null
  var countOfRecievedMessage: Int = 0
  var sum: Double = 1
  var weight: Double = 1
  var oldRatio: Double = 0
  var newRatio: Double = 0
  var numOfRounds: Int = 0
  var finishProtocol: Boolean = false
  var reachedLastNode: Boolean = false
  var reachedNextNode: Boolean = false
  var actorNode2 = new ArrayBuffer[ActorRef]()

  def updateGossipRecieved(nodeNumberSelected : Int) = {
      countOfRecievedMessage = countOfRecievedMessage + 1
      println("countOfRecievedMessage" +countOfRecievedMessage)
      if(countOfRecievedMessage > 10){
        //println("hhh--in loop"  + master)
        //context.system.shutdown()
        //master ! ConvergenceAchieved()
        master ! ConvergenceAchieved
      }
  }

  def neighbourForLine(selectedNode : Int,totalNodes : Int) : Int = {
      println("totalNodes" + totalNodes + "selectedNode" + selectedNode)
      //return 1;
      var selectedNodeToContinue : Int = 0
      if (selectedNode == 0){
        selectedNodeToContinue = 1
      }else if(selectedNode >= 1 && selectedNode <= totalNodes - 2){
        selectedNodeToContinue = Random.shuffle((Array(selectedNode-1, selectedNode+1)).toList).head
      }else if(selectedNode == (totalNodes - 1)){
        selectedNodeToContinue = totalNodes - 2
      }
      return selectedNodeToContinue;
  }

  def neighbourForFull(selectedNode : Int, totalNodes : Int) : Int = {
      println("totalNodes" + totalNodes + "selectedNode" + selectedNode)

      var selectedNodeToContinue : Int = 0
      var i : Int = 0
      var found : Boolean = false

      while (i < 3 && !found){
        selectedNodeToContinue = Random.nextInt(totalNodes)
        if(selectedNodeToContinue != selectedNode){
          found = true
        }
        i = i + 1
      }

      if(selectedNodeToContinue == selectedNode){
        selectedNodeToContinue = neighbourForLine(selectedNode, totalNodes)
      }

      return selectedNodeToContinue  
  }

  def neighbourFor3d(selectedNode : Int, totalNodes : Int) : Int = {
      println("totalNodes" + totalNodes + "selectedNode" + selectedNode)

      var selectedNodeToContinue : Int = 0

      val cuberoot : Int = cbrt(totalNodes).toInt

      var squareOfcuberoot : Int = pow(cuberoot,2).toInt

      var findPlane : Int = (selectedNode/squareOfcuberoot).toInt

      var n : Int = cuberoot*(cuberoot-1)

      var k : Int = cuberoot*cuberoot - 1

      if(findPlane == 0){

        if(selectedNode == 0){
          selectedNodeToContinue = Random.shuffle((Array(1, cuberoot, cuberoot*cuberoot)).toList).head
        }

        else if ( selectedNode >= 1 && selectedNode <= cuberoot - 2){
          selectedNodeToContinue = Random.shuffle(Array((selectedNode-1) ,(selectedNode+1), (selectedNode+cuberoot), (selectedNode+cuberoot*cuberoot)).toList).head
        }

        else if(selectedNode == cuberoot-1){
          selectedNodeToContinue = Random.shuffle(Array((cuberoot - 2),(cuberoot-1 + cuberoot), (cuberoot-1 + cuberoot*cuberoot)).toList).head
        }

        else if(selectedNode >= cuberoot && selectedNode <= cuberoot*(cuberoot- 1) - 1) {
          if (selectedNode % cuberoot == 0) {
            selectedNodeToContinue = Random.shuffle(Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode + 1), (selectedNode + cuberoot*cuberoot))toList).head
          } 
          else if (selectedNode % cuberoot == cuberoot - 1) {
            selectedNodeToContinue = Random.shuffle(Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode - 1), (selectedNode + cuberoot*cuberoot))toList).head
          } 
          else {
            selectedNodeToContinue = Random.shuffle(Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode - 1), (selectedNode + 1), (selectedNode + cuberoot*cuberoot))toList).head
          }
        }
        
        else if(n == selectedNode){
          selectedNodeToContinue = Random.shuffle(Array((n - cuberoot), (n + 1), (n + cuberoot*cuberoot)).toList).head
        }

        else if(selectedNode >= cuberoot*(cuberoot-1) + 1 && selectedNode <= cuberoot*cuberoot - 2) {
          selectedNodeToContinue = Random.shuffle(Array((selectedNode - 1), (selectedNode + 1), (selectedNode - cuberoot), (selectedNode + cuberoot*cuberoot))toList).head
        }

        else if(selectedNode == k){
          selectedNodeToContinue = Random.shuffle(Array((k - 1), (k - cuberoot), (k + cuberoot*cuberoot)).toList).head  
        }     
      }

      else if(findPlane == cuberoot - 1){

        var n : Int = totalNodes-(cuberoot*cuberoot)
        var k : Int = totalNodes-(cuberoot*cuberoot)+cuberoot-1
        var l : Int = totalNodes - cuberoot
        var m : Int = totalNodes - 1

        if(n == selectedNode){
          selectedNodeToContinue = Random.shuffle(Array((n+1), (n+cuberoot), (n - cuberoot*cuberoot))toList).head
        }
          
        else if(selectedNode >= totalNodes-(cuberoot*cuberoot)+1 && selectedNode <= totalNodes-(cuberoot*cuberoot)+cuberoot - 2){
          selectedNodeToContinue = Random.shuffle(Array((selectedNode-1), (selectedNode+1), (selectedNode+cuberoot), (selectedNode- cuberoot*cuberoot))toList).head
        }
        
        else if(k == selectedNode){        
          selectedNodeToContinue = Random.shuffle(Array((k - 1), (k + cuberoot), (k - cuberoot*cuberoot))toList).head
        }

        else if(selectedNode >= totalNodes-(cuberoot*cuberoot)+cuberoot && selectedNode <= totalNodes - cuberoot - 1) { 
          if (selectedNode % cuberoot == 0) {
            selectedNodeToContinue = Random.shuffle(Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode + 1), (selectedNode - cuberoot*cuberoot))toList).head
          } 
          else if (selectedNode % cuberoot == cuberoot - 1) {
            selectedNodeToContinue = Random.shuffle(Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode - 1), (selectedNode - cuberoot*cuberoot))toList).head
          } 
          else {
            selectedNodeToContinue = Random.shuffle(Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode - 1), (selectedNode + 1), (selectedNode - cuberoot*cuberoot))toList).head
          }
        }

        else if(l == selectedNode){
          selectedNodeToContinue = Random.shuffle(Array((l - cuberoot), (l + 1), (l - cuberoot*cuberoot))toList).head
        }

        else if(selectedNode >= totalNodes - cuberoot + 1 && selectedNode <= totalNodes - 2) {
          selectedNodeToContinue = Random.shuffle(Array((selectedNode - 1), (selectedNode + 1), (selectedNode - cuberoot), (selectedNode - cuberoot*cuberoot))toList).head
        }

        else if(m == selectedNode){
          selectedNodeToContinue = Random.shuffle(Array((m - 1), (m - cuberoot), (m - cuberoot*cuberoot))toList).head
        }     
      }

      else{

        var n : Int = cuberoot * cuberoot * findPlane
        var m : Int = n + cuberoot*(cuberoot-1)
        var k : Int = n + cuberoot*cuberoot - 1

        if(selectedNode == n){
          selectedNodeToContinue = Random.shuffle(Array((n + 1), (n + cuberoot), (n + cuberoot*cuberoot), (n - cuberoot*cuberoot))toList).head
        }

        else if (selectedNode >= n + 1 && selectedNode <= n + cuberoot - 2){
          selectedNodeToContinue = Random.shuffle(Array((selectedNode - 1), (selectedNode + 1), (selectedNode + cuberoot), (selectedNode + cuberoot*cuberoot), (selectedNode - cuberoot*cuberoot))toList).head
        }

        else if (selectedNode == (n + cuberoot-1)){
          selectedNodeToContinue = Random.shuffle(Array((n + cuberoot-2), (n + cuberoot-1 + cuberoot), (n + cuberoot-1 + cuberoot*cuberoot), (n + cuberoot-1 - cuberoot*cuberoot))toList).head
        }

        else if(selectedNode >= (n + cuberoot) && selectedNode <= (n + cuberoot*(cuberoot - 1) - 1)) { 
          if (selectedNode % cuberoot == 0) {
            selectedNodeToContinue = Random.shuffle(Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode + 1), (selectedNode + cuberoot*cuberoot), (selectedNode - cuberoot*cuberoot))toList).head
          } 
          else if (selectedNode % cuberoot == cuberoot - 1) {
            selectedNodeToContinue = Random.shuffle(Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode - 1), (selectedNode + cuberoot*cuberoot), (selectedNode - cuberoot*cuberoot))toList).head
          } 
          else {
            selectedNodeToContinue = Random.shuffle(Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode - 1), (selectedNode + 1), (selectedNode + cuberoot*cuberoot), (selectedNode - cuberoot*cuberoot))toList).head
          }
        }

        if(selectedNode == m){
          selectedNodeToContinue = Random.shuffle(Array((m - cuberoot), (m + 1), (m + cuberoot*cuberoot), (m - cuberoot*cuberoot))toList).head
        }
  
        if(selectedNode >= (m + 1) && selectedNode <= (n + cuberoot*cuberoot - 2)) {
          selectedNodeToContinue = Random.shuffle(Array((selectedNode - 1), (selectedNode + 1), (selectedNode - cuberoot), (selectedNode + cuberoot*cuberoot), (selectedNode - cuberoot*cuberoot))toList).head
        }

        if(selectedNode == k){
          selectedNodeToContinue = Random.shuffle(Array((k - 1), (k - cuberoot), (k + cuberoot*cuberoot), (k - cuberoot*cuberoot))toList).head
        } 
      }      
    return selectedNodeToContinue
  } 

  def neighbourForImp3d (selectedNode : Int, totalNodes : Int) : Int = {
    println("totalNodes" + totalNodes + "selectedNode" + selectedNode)

    var selectedNodeToContinue : Int = 0
    val cuberoot : Int = ceil(cbrt(totalNodes)).toInt
    var squareOfcuberoot : Int = pow(cuberoot,2).toInt
    var findPlane : Int = (selectedNode/squareOfcuberoot).toInt
    var n : Int = cuberoot*(cuberoot-1)
    var k : Int = cuberoot*cuberoot - 1
    var randomNeighbor: Int = 0
    var totalNodesArray = (0 to totalNodes-1).toSet

    if(findPlane == 0){

      if(selectedNode == 0){
        var s : Set[Int] = Set(1, cuberoot, cuberoot*cuberoot)
        val diff = totalNodesArray.diff(s)
        selectedNodeToContinue = Random.shuffle(List.empty[Int] ++ diff).head
        //SetRandomNeighbor(selectedNode)
      }

      // else if ( selectedNode >= 1 && selectedNode <= cuberoot - 2){
      //   selectedNodeToContinue = Random.shuffle( totalNodesArray - Array ( (selectedNode-1) ,(selectedNode+1), (selectedNode+cuberoot), (selectedNode+cuberoot*cuberoot)).toList).head
      //   //SetRandomNeighbor(selectedNode)
      // }

      // else if(selectedNode == cuberoot-1){
      //   selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((cuberoot - 2),(cuberoot-1 + cuberoot), (cuberoot-1 + cuberoot*cuberoot))).toList).head
      //   //SetRandomNeighbor(selectedNode)
      // }

      //   else if(selectedNode >= cuberoot && selectedNode <= cuberoot*(cuberoot- 1) - 1) {
      //     if (selectedNode % cuberoot == 0) {
      //       selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode + 1), (selectedNode + cuberoot*cuberoot))).toList).head
      //       //SetRandomNeighbor(selectedNode)
      //     } 
      //     else if (selectedNode % cuberoot == cuberoot - 1) {
      //       selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode - 1), (selectedNode + cuberoot*cuberoot))).toList).head
      //       //SetRandomNeighbor(selectedNode)
      //     } 
      //     else {
      //       selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode - 1), (selectedNode + 1), (selectedNode + cuberoot*cuberoot))).toList).head
      //       //SetRandomNeighbor(selectedNode)
      //     }
      //   }
        
      //   else if(n == selectedNode){
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((n - cuberoot), (n + 1), (n + cuberoot*cuberoot))).toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   }

      //   else if(selectedNode >= cuberoot*(cuberoot-1) + 1 && selectedNode <= cuberoot*cuberoot - 2) {
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode - 1), (selectedNode + 1), (selectedNode - cuberoot), (selectedNode + cuberoot*cuberoot))).toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   }

      //   else if(selectedNode == k){
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((k - 1), (k - cuberoot), (k + cuberoot*cuberoot))).toList).head  
      //     //SetRandomNeighbor(selectedNode)
      //   }     
      // }

      // else if(findPlane == cuberoot - 1){

      //   var n : Int = totalNodes-(cuberoot*cuberoot)
      //   var k : Int = totalNodes-(cuberoot*cuberoot)+cuberoot-1
      //   var l : Int = totalNodes - cuberoot
      //   var m : Int = totalNodes - 1

      //   if(n == selectedNode){
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((n+1), (n+cuberoot), (n - cuberoot*cuberoot))).toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   }
          
      //   else if(selectedNode >= totalNodes-(cuberoot*cuberoot)+1 && selectedNode <= totalNodes-(cuberoot*cuberoot)+cuberoot - 2){
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode-1), (selectedNode+1), (selectedNode+cuberoot), (selectedNode- cuberoot*cuberoot))).toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   }
        
      //   else if(k == selectedNode){        
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((k - 1), (k + cuberoot), (k - cuberoot*cuberoot))).toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   }

      //   else if(selectedNode >= totalNodes-(cuberoot*cuberoot)+cuberoot && selectedNode <= totalNodes - cuberoot - 1) { 
      //     if (selectedNode % cuberoot == 0) {
      //       selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode + 1), (selectedNode - cuberoot*cuberoot))).toList).head
      //       //SetRandomNeighbor(selectedNode)
      //     } 
      //     else if (selectedNode % cuberoot == cuberoot - 1) {
      //       selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode - 1), (selectedNode - cuberoot*cuberoot))).toList).head
      //       //SetRandomNeighbor(selectedNode)
      //     } 
      //     else {
      //       selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode - 1), (selectedNode + 1), (selectedNode - cuberoot*cuberoot))).toList).head
      //       //SetRandomNeighbor(selectedNode)
      //     }
      //   }

      //   else if(l == selectedNode){
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((l - cuberoot), (l + 1), (l - cuberoot*cuberoot))).toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   }

      //   else if(selectedNode >= totalNodes - cuberoot + 1 && selectedNode <= totalNodes - 2) {
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode - 1), (selectedNode + 1), (selectedNode - cuberoot), (selectedNode - cuberoot*cuberoot))).toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   }

      //   else if(m == selectedNode){
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((m - 1), (m - cuberoot), (m - cuberoot*cuberoot))).toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   }     
      // }

      // else{

      //   var n : Int = cuberoot * cuberoot * findPlane
      //   var m : Int = n + cuberoot*(cuberoot-1)
      //   var k : Int = n + cuberoot*cuberoot - 1

      //   if(selectedNode == n){
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((n + 1), (n + cuberoot), (n + cuberoot*cuberoot), (n - cuberoot*cuberoot))).toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   }

      //   else if (selectedNode >= n + 1 && selectedNode <= n + cuberoot - 2){
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode - 1), (selectedNode + 1), (selectedNode + cuberoot), (selectedNode + cuberoot*cuberoot), (selectedNode - cuberoot*cuberoot))).toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   }

      //   else if (selectedNode == (n + cuberoot-1)){
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((n + cuberoot-2), (n + cuberoot-1 + cuberoot), (n + cuberoot-1 + cuberoot*cuberoot), (n + cuberoot-1 - cuberoot*cuberoot))).toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   }

      //   else if(selectedNode >= (n + cuberoot) && selectedNode <= (n + cuberoot*(cuberoot - 1) - 1)) { 
      //     if (selectedNode % cuberoot == 0) {
      //       selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode + 1), (selectedNode + cuberoot*cuberoot), (selectedNode - cuberoot*cuberoot))).toList).head
      //       //SetRandomNeighbor(selectedNode)
      //     } 
      //     else if (selectedNode % cuberoot == cuberoot - 1) {
      //       selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode - 1), (selectedNode + cuberoot*cuberoot), (selectedNode - cuberoot*cuberoot))).toList).head
      //       //SetRandomNeighbor(selectedNode)
      //     } 
      //     else {
      //       selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode - cuberoot), (selectedNode + cuberoot), (selectedNode - 1), (selectedNode + 1), (selectedNode + cuberoot*cuberoot), (selectedNode - cuberoot*cuberoot))).toList).head
      //       //SetRandomNeighbor(selectedNode)
      //     }
      //   }

      //   if(selectedNode == m){
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((m - cuberoot), (m + 1), (m + cuberoot*cuberoot), (m - cuberoot*cuberoot))).toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   }
  
      //   if(selectedNode >= (m + 1) && selectedNode <= (n + cuberoot*cuberoot - 2)) {
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((selectedNode - 1), (selectedNode + 1), (selectedNode - cuberoot), (selectedNode + cuberoot*cuberoot), (selectedNode - cuberoot*cuberoot))).toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   }

      //   if(selectedNode == k){
      //     selectedNodeToContinue = Random.shuffle((totalNodesArray - Array((k - 1), (k - cuberoot), (k + cuberoot*cuberoot), (k - cuberoot*cuberoot))).toList).head
      //     //randomNeighbor = Random.shuffle(Array -- Array((k - 1), (k - cuberoot), (k + cuberoot*cuberoot), (k - cuberoot*cuberoot))toList).head
      //     //SetRandomNeighbor(selectedNode)
      //   } 
      // }      
    return selectedNodeToContinue

    // def SetRandomNeighbor(number : Int){
    //       if (clonedWorkersList.contains(actor(number)) && clonedWorkersList.length >= 2) {
    //         randomNeighbor = (nodesArray -- neighborArray)(Random.nextInt((nodesArray -- neighborArray).length)) 
    //         //randomNeighbor = (clonedWorkersList - actor(number) -- neighborArray)(Random.nextInt((clonedWorkersList - actor(number) -- neighborArray).length))
    //         randomness += (randomNeighbor -> number) //Map storing the values of the actor and its random neighbor
    //         actor(number) ! AddRandomNeighbor(randomNeighbor)
    //         clonedWorkersList -= (actor(number), actor(randomNeighbor))
    //       }

    //       else if(!(clonedWorkersList.contains(actor(number)))){
    //         foundActor = randomness.apply(number)
    //         actor(number) ! AddRandomNeighbor(foundActor)
    //       }
    //     }
    }
}
  def giveMeMyRandomNeighbour(i: Int, totalCalculatedNodes:Int,topologyOfNetwork:String): Int = {
    var randomNeighborToContinue:Int = 0
    if(topologyOfNetwork == "line"){
      randomNeighborToContinue = neighbourForLine(i,totalCalculatedNodes)
    }else if(topologyOfNetwork == "full"){
      randomNeighborToContinue = neighbourForFull(i,totalCalculatedNodes)
    }else if(topologyOfNetwork == "3d"){
      randomNeighborToContinue = neighbourFor3d(i,totalCalculatedNodes)
    }else if(topologyOfNetwork == "imp3d"){
      randomNeighborToContinue = neighbourForImp3d(0,totalCalculatedNodes)
    }
    return randomNeighborToContinue   
  }

  def receive = {
    case StartGossip(totalCalculatedNodes,topologyOfNetwork,invokedFirstTime,participatingNodes) => {

      println("GossipStarted")
      println(totalCalculatedNodes + "hhh" + topologyOfNetwork)
      //Get the node number from path name
      var name = self.path.name
      println(self.path.name)
      var nodeNumberSelectedString = name.split(":").last
      var nodeNumberSelected = nodeNumberSelectedString.toInt
      println(nodeNumberSelected)
      //updateGossipRecieved(nodeNumberSelected)
      countOfRecievedMessage = countOfRecievedMessage + 1
      if(countOfRecievedMessage < 10){
        var randomNeighborToContinue = giveMeMyRandomNeighbour(nodeNumberSelected,totalCalculatedNodes,topologyOfNetwork)
        participatingNodes(randomNeighborToContinue) ! StartGossip(totalCalculatedNodes,topologyOfNetwork,0,participatingNodes)
        context.system.scheduler.scheduleOnce(500 milliseconds, self, StartGossip(totalCalculatedNodes,topologyOfNetwork,0,participatingNodes))
        println("Next Neighbor is : " + randomNeighborToContinue) 
      }else{
        master ! ConvergenceAchieved
      }
      
        //Choose a random neighbour according to the network type specified and count

      


    }
    
    case StartPushSum(totalCalculatedNodes,topologyOfNetwork,sumNew,weightNew,invokedFirstTime,participatingNodes) => {

    }      

    case ResetSum(s) => {
      //println("s = " +s)
      sum = s
      oldRatio = sum / weight
      master = sender
      //println(master)
    } 

  }
}

 
object project2 extends App {
  override def main(args: Array[String]) {
    // Defining the system of actors (both master and worker)
    implicit val system = ActorSystem("networkTopologySystem")
    var algoSelected = args(0)
    var totalNodesEntered = args(1).toDouble
    var topologyOfNetwork = args(2)
    println("the algoSelected : " + algoSelected)
    println("the totalNodes : " + totalNodesEntered)
    println("the topologyOfNetwork : " + topologyOfNetwork)
    val networkMaster = system.actorOf(Props(new networkMaster(algoSelected,totalNodesEntered,topologyOfNetwork)))
    //implicit val timeout = Timeout(25 seconds) 
    var topologyCreated = networkMaster ! CreateTopologyOfNodes()
    //println(topologyCreated)
    //if(topologyCreated == 1){
    println("topology Created successfully!!")

  }
}