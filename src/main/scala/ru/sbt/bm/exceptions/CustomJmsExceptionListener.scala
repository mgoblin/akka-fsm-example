package ru.sbt.bm.exceptions

import javax.jms.{JMSException, ExceptionListener}

import com.ibm.mq.MQException
import org.slf4j.{LoggerFactory, Logger}

/**
 * JMS exceptions listener.
 *
 * Extract from JMS exception underlying MQ exception and rethrow MQ exception.
 * Use as a part of error handling MQ manager unavailable case.
 * Without this listener Apache Camel try to reconnect to MQ manager every 5 seconds.
 *
 * MQ exception forward to Akka actor [[ru.sbt.bm.source.MQEndpoint]]
 * MQ endpoint supervisor stops MQ endpoint if MQ manager unavailable.
 */
class CustomJmsExceptionListener extends ExceptionListener {
  private val logger = LoggerFactory.getLogger(getClass)

  override def onException(ex: JMSException): Unit = {
    ex.getCause match {
      case e: MQException =>
        import MQErrors._
        if (e.completionCode == ErrorCompletionCode && e.reasonCode == MQManagerNotAvail) {
          logger.error("MQ manager unavailable", e)
          throw e
        } else {
          logger.error("MQ exception", e)
        }
    }
  }
}

/**
 * Define MQ error codes
 */
object MQErrors {
  /** MQ operation error */
  val ErrorCompletionCode = 2
  /** MQ manager unavailable error reason*/
  val MQManagerNotAvail = 2059
}
