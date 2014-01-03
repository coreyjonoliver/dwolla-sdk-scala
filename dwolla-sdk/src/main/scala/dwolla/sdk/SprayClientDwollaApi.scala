package dwolla.sdk

import scala.concurrent.{ExecutionContext, Future}
import akka.actor.ActorSystem
import spray.client.pipelining._
import akka.util.Timeout
import spray.http._
import DwollaApiResponseJsonProtocol._
import spray.json._
import spray.can.client.HostConnectorSettings
import spray.can.Http.HostConnectorSetup
import spray.httpx.SprayJsonSupport._
import dwolla.sdk.DwollaApiRequestJsonProtocol.{AddFundingSourceRequest, SendAsGuestRequest, RefundRequest, SendRequest}

class SprayClientDwollaApi(settings: Option[HostConnectorSettings] = None)(
  implicit system: ActorSystem,
  timeout: Timeout,
  ec: ExecutionContext) extends SprayHttpClient with DwollaApi {

  private val setup = HostConnectorSetup(
    host = "www.dwolla.com",
    port = 443,
    sslEncryption = true,
    settings = settings
  )

  private val requestPreparer: RequestTransformer = addHeader("Accept", "application/json")

  private def execute[T](req: HttpRequest): Future[HttpResponse] = pipeline(setup).flatMap(_(requestPreparer(req)))

  private def executeTo[T](req: HttpRequest, m: (HttpResponse => T)): Future[T] = execute(req).map(m)

  private def mapResponse[T: JsonFormat](response: HttpResponse) = {
    if (response.status.isSuccess) {
      val parsedResponse = response.entity.asString.asJson.convertTo[Response[T]]
      if (parsedResponse.success) {
        parsedResponse.response.get
      } else {
        throw new DwollaException(parsedResponse.message)
      }
    } else {
      throw new DwollaException("Unsuccessful response: " + response.entity.asString)
    }
  }

  def getBalance(accessToken: String): Future[GetBalanceResponse] = {
    val uri = Uri("/oauth/rest/balance/").withQuery(Map("oauth_token" -> accessToken))
    executeTo(Get(uri), mapResponse[GetBalanceResponse])
  }

  def addFundingSource(accessToken: String, accountNumber: String, routingNumber: String, accountType: String,
                       name: String): Future[AddFundingSourceResponse] = {
    val uri = Uri("/oauth/rest/fundingsources")
    val raw = AddFundingSourceRequest(accessToken, accountNumber, routingNumber, accountType, name)
    executeTo(Post(uri, raw), mapResponse[AddFundingSourceResponse])
  }

  def getFundingSourceDetails(accessToken: String, fundingId: Int) = {
    val uri = Uri(s"/fundingsources/$fundingId").withQuery(Map("oauth_token" -> accessToken))
    executeTo(Get(uri), mapResponse[GetFundingSourceDetailsResponse])
  }

  def getTransactionDetails(accessToken: String, transactionId: Int) = {
    val uri = Uri(s"/oauth/rest/transactions/$transactionId").withQuery(Map("oauth_token" -> accessToken))
    executeTo(Get(uri), mapResponse[GetTransactionDetailsResponse])
  }

  def sendMoneyAsGuest(clientId: String, clientSecret: String, destinationId: String, amount: BigDecimal,
                       firstName: String, lastName: String, emailAddress: String, routingNumber: String,
                       accountNumber: String, accountType: String, assumeCosts: Option[Boolean] = None,
                       destinationType: Option[String] = None, notes: Option[String] = None, groupId: Option[Int],
                       additionalFees: Seq[FacilitatorFee] = List(), facilitatorAmount: Option[BigDecimal] = None,
                       assumeAdditionalFees: Option[Boolean] = None) = {
    val uri = Uri("/oauth/rest/transactions/guestsend")
    val raw = SendAsGuestRequest(clientId, clientSecret, destinationId, amount, firstName, lastName, emailAddress,
      routingNumber, accountNumber, accountType, assumeCosts, destinationType, notes, groupId, additionalFees,
      facilitatorAmount, assumeAdditionalFees)
    executeTo(Post(uri, raw), mapResponse[SendMoneyAsGuestResponse])
  }

  def listAllTransactions(accessToken: String) = {
    val uri = Uri(s"/oauth/rest/transactions/").withQuery(Map("oauth_token" -> accessToken))
    executeTo(Get(uri), mapResponse[ListAllTransactionsResponse])
  }

  def issueRefund(accessToken: String, pin: String, transactionId: Int, fundsSource: Int, amount: BigDecimal,
                  notes: Option[String] = None) = {
    val uri = Uri("/oauth/rest/transactions/refund")
    val raw = RefundRequest(accessToken, pin, transactionId, fundsSource, amount, notes)
    executeTo(Post(uri, raw), mapResponse[IssueRefundResponse])
  }

  def sendMoney(accessToken: String, pin: String, destinationId: String, amount: BigDecimal,
                destinationType: Option[String] = None,
                facilitatorAmount: Option[BigDecimal] = None, assumeCosts: Option[Boolean] = None,
                notes: Option[String] = None,
                additionalFees: Seq[FacilitatorFee] = List(), assumeAdditionalFees: Option[Boolean] = None):
  Future[SendMoneyResponse] = {
    val uri = Uri("/oauth/rest/transactions/send")
    val raw = SendRequest(accessToken, pin, destinationId, amount, destinationType, facilitatorAmount, assumeCosts,
      notes, additionalFees, assumeAdditionalFees)
    executeTo(Post(uri, raw), mapResponse[SendMoneyResponse])
  }

  def getFullAccountInformation(accessToken: String): Future[FullAccountInformationResponse] = {
    val uri = Uri("/oauth/rest/users/").withQuery(Map("oauth_token" -> accessToken))
    executeTo(Get(uri), mapResponse[FullAccountInformationResponse])
  }

  def getBasicAccountInformation(clientId: String, clientSecret: String,
                                 accountIdentifier: String): Future[BasicAccountInformationResponse] = {
    val uri = Uri(s"/oauth/rest/users/$accountIdentifier")
    val uriWithQuery = uri.withQuery(Map("client_id" -> clientId, "client_secret" -> clientSecret))
    executeTo(Get(uriWithQuery), mapResponse[BasicAccountInformationResponse])
  }

  def findUsersNearby(clientId: String, clientSecret: String, latitude: BigDecimal,
                      longitude: BigDecimal): Future[FindUsersNearbyResponse] = {
    val uri = Uri("/oauth/rest/users/nearby")
    val uriWithQuery = uri.withQuery(Map("client_id" -> clientId, "client_secret" -> clientSecret,
      "latitude" -> latitude.toString,
      "longitude" -> longitude.toString))
    executeTo(Get(uriWithQuery), mapResponse[FindUsersNearbyResponse])
  }
}
