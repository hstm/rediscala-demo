import akka.util.ByteString
import redis.RedisClient
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.util.{Failure, Success}

object ExampleTransaction extends App {
  implicit val akkaSystem = akka.actor.ActorSystem()

  val redis = RedisClient()

  val redisTransaction = redis.transaction()
  redisTransaction.watch("key")
  val set = redisTransaction.set("key", "abcValue")
  val decr = redisTransaction.decr("key")
  val get = redisTransaction.get("key")
  redisTransaction.exec()
  val r = for {
    s <- set
    g <- get
  } yield {
    assert(s)
    println("ok : set(\"key\", \"abcValue\")")
    assert(g == Some(ByteString("abcValue")))
    println("ok : get(\"key\") == \"abcValue\"")
  }
  decr.onComplete({
    case Success(value) => println(s"decr: $value")
    case Failure(e) => println(s"decr failed : $e")
  })
  Await.result(r, 10 seconds)

  akkaSystem.terminate()
}
