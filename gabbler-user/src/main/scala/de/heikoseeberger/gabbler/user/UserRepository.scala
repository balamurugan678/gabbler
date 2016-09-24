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

import akka.actor.{ Actor, ActorLogging, Props }

object UserRepository {

  final case class User(id: Long,
                        username: String,
                        nickname: String,
                        email: String)

  final val Name = "user-repository"

  def props: Props = Props(new UserRepository)
}

final class UserRepository extends Actor with ActorLogging {
  import UserRepository._

  override def receive = ???

  private def handleAddUser(username: String,
                            nickname: String,
                            email: String) =
    ???

  private def handleRemoveUser(id: Long) = {
    ???
  }
}
