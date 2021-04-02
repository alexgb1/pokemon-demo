package org.pokemon

import cats.data.EitherT
import cats.effect.{ExitCode, IO, IOApp, Sync}
import cats.syntax.applicative._
import cats.syntax.either._
import cats.syntax.functor._
import cats.syntax.semigroupk._
import fs2.text.utf8Decode
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.{HttpRoutes, Status, Uri}
import sttp.model.StatusCode
import sttp.tapir._
import sttp.tapir.docs.openapi.OpenAPIDocsInterpreter
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import sttp.tapir.openapi.circe.yaml.RichOpenAPI
import sttp.tapir.openapi.{Info, OpenAPI}
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.http4s.SwaggerHttp4s

import scala.concurrent.ExecutionContext.global

object Server extends IOApp {

  case class Pokemon(description: String, imageUrl: String)

  val internalError = statusMapping(StatusCode.InternalServerError, jsonBody[InternalError])
  val notFoundError = statusMapping(StatusCode.NotFound, jsonBody[NotFoundError])

  val getPokemonEndpoint = endpoint.get
    .in("pokemon")
    .in(query[String]("name"))
    .out(jsonBody[Pokemon])
    .errorOut(oneOf[ApiError](internalError, notFoundError))

  val docs: OpenAPI =
    OpenAPIDocsInterpreter.toOpenAPI(Iterable(getPokemonEndpoint), Info("Pokemon API", "1"))
  val docsRoute: HttpRoutes[IO] = new SwaggerHttp4s(docs.toYaml).routes

  sealed trait ApiError {
    def message: String
  }

  object ApiError {
    def internalError(message: String): ApiError = InternalError(message)

    def notFoundError(message: String): ApiError = NotFoundError(message)
  }

  case class InternalError(message: String) extends ApiError

  case class NotFoundError(message: String) extends ApiError


  // http client

  case class Sprites(front_default: String)

  case class Specie(name: String, url: String)

  case class PokemonResponse(species: Specie, sprites: Sprites)

  case class Language(name: String)

  case class Flavor(flavor_text: String, language: Language)

  case class SpeciesResponse(flavor_text_entries: List[Flavor])

  trait PokemonRetriever[F[_]] {
    def retrievePokemonUrl(name: String): F[Either[ApiError, Pokemon]]
  }

  object PokemonRetriever {

    def apply[F[_] : Sync](client: Client[F], uri: Uri): PokemonRetriever[F] = new PokemonRetriever[F] {
      override def retrievePokemonUrl(name: String): F[Either[ApiError, Pokemon]] = {
        (for {
          pokemonResponse <- EitherT(client.get(uri / "pokemon" / name) {
            case Status.Successful(r) =>
              r.attemptAs[PokemonResponse].leftMap(_.message).leftMap(ApiError.internalError).value
            case Status.NotFound(_) =>
              ApiError.notFoundError(s"Pokemon ${name} not found").asLeft[PokemonResponse].pure
            case failure => failure.body.through(utf8Decode).compile.string.map(ApiError.internalError).map(_.asLeft[PokemonResponse])
          })
          speciesUrl <- EitherT(Uri.fromString(pokemonResponse.species.url)
            .leftMap(failure => InternalError(failure.message)).pure)
          speciesResponse <- EitherT.right[ApiError](client.expect[SpeciesResponse](speciesUrl))
          description = speciesResponse
            .flavor_text_entries
            .find(_.language.name == "en")
            .fold("No description")(_.flavor_text.trim)
        } yield Pokemon(
          description = description,
          imageUrl = pokemonResponse.sprites.front_default
        )).value
      }
    }
  }


  override def run(args: List[String]): IO[ExitCode] = {
    (for {
      route <- BlazeClientBuilder[IO](global).stream.map(client => {
        val pokemonRetriever = PokemonRetriever[IO](client, uri"https://pokeapi.co/api/v2/")
        Http4sServerInterpreter.toRoutes(getPokemonEndpoint) { pokemonName =>
          pokemonRetriever.retrievePokemonUrl(pokemonName)
        }
      })
      _ <- BlazeServerBuilder[IO](executionContext)
        .bindHttp(9090, "localhost")
        .withHttpApp((route <+> docsRoute).orNotFound)
        .serve
    } yield ()).compile.drain.as(ExitCode.Success)
  }

}
