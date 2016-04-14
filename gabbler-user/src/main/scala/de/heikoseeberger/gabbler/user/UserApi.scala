/*
 * Copyright 2016 Heiko Seeberger
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.heikoseeberger.gabbler.user

import akka.actor.Status.Failure
import akka.actor.{ Actor, ActorLogging, ActorRef, Props }
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.{
  BadRequest,
  Conflict,
  Created,
  NoContent,
  NotFound
}
import akka.http.scaladsl.model.headers.Location
import akka.http.scaladsl.model.{ HttpEntity, HttpResponse }
import akka.http.scaladsl.server.Directives
import akka.pattern.{ ask, pipe }
import akka.stream.ActorMaterializer
import akka.util.Timeout
import de.heikoseeberger.akkahttpcirce.CirceSupport
import de.heikoseeberger.akkasse.MediaTypes.`text/event-stream`
import de.heikoseeberger.akkasse.headers.`Last-Event-ID`
import de.heikoseeberger.akkasse.{ EventStreamMarshalling, ServerSentEvent }
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets.UTF_8
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object UserApi {

  final val Name = "user-api"

  def props(address: String,
            port: Int,
            userRepository: ActorRef,
            userRepositoryTimeout: FiniteDuration): Props =
    Props(new UserApi(address, port, userRepository)(userRepositoryTimeout))

  private[user] def apply(
      userRepository: ActorRef
  )(implicit userRepositoryTimeout: Timeout, ec: ExecutionContext) = {
    import CirceSupport._
    import Directives._
    import EventStreamMarshalling._
    import UserRepository._
    import io.circe.generic.auto._
    import io.circe.syntax._

    // format: OFF
    def users = pathPrefix("users") {
      pathEnd {
        get {
          complete {
            (userRepository ? GetUsers).mapTo[Users].map(_.users)
          }
        } ~
        post {
          entity(as[AddUser]) { addUser =>
            extractUri { uri =>
              onSuccess(userRepository ? addUser) {
                case UsernameTaken(username) => complete(Conflict -> s"Username $username taken!")
                case UserAdded(user)         => complete((Created, Vector(Location(uri.withPath(uri.path / user.id.toString))), user))
              }
            }
          }
        }
      } ~
      path(LongNumber) { id =>
        delete {
          onSuccess(userRepository ? RemoveUser(id)) {
            case IdUnknown(_)  => complete(NotFound -> s"User with id $id not found!")
            case UserRemoved(_) => complete(NoContent)
          }
        }
      }
    }

    def userEvents = {
      def userEventToServerSentEvent(userEvent: (Long, UserEvent)) = userEvent match {
        case (seqNo, UserAdded(user))   => ServerSentEvent(user.asJson.noSpaces, "user-added", seqNo.toString)
        case (seqNo, UserRemoved(user)) => ServerSentEvent(user.asJson.noSpaces, "user-removed", seqNo.toString)
      }
      path("user-events") {
        get {
          optionalHeaderValueByName(`Last-Event-ID`.name) { lastEventId =>
            try {
              val fromSeqNo = lastEventId.getOrElse("0").trim.toLong + 1
              complete {
                (userRepository ? GetUserEvents(fromSeqNo))
                  .mapTo[UserEvents]
                  .map(_.userEvents.map(userEventToServerSentEvent))
              }
            } catch {
              case e: NumberFormatException =>
                complete(HttpResponse(BadRequest, entity = HttpEntity(
                  `text/event-stream`,
                  "Integral number expected for Last-Event-ID header!".getBytes(UTF_8)
                )))
            }
          }
        }
      }
    }
    // format: ON

    users ~ userEvents
  }
}

final class UserApi(address: String, port: Int, userRepository: ActorRef)(
    implicit userRepositoryTimeout: Timeout
) extends Actor
    with ActorLogging {
  import context.dispatcher

  private implicit val mat = ActorMaterializer()

  Http(context.system)
    .bindAndHandle(UserApi(userRepository), address, port)
    .pipeTo(self)

  override def receive = {
    case Http.ServerBinding(socketAddress) => handleBinding(socketAddress)
    case Failure(cause)                    => handleBindFailure(cause)
  }

  private def handleBinding(socketAddress: InetSocketAddress) = {
    log.info(s"Listening on $socketAddress")
    context.become(Actor.emptyBehavior)
  }

  private def handleBindFailure(cause: Throwable) = {
    log.error(cause, s"Can't bind to $address:$port!")
    context.stop(self)
  }
}
