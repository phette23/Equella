module OEQ.API.Requests where 

import Prelude

import Control.Alt ((<|>))
import Data.Argonaut (Json, jsonParser)
import Data.Bifunctor (lmap)
import Data.Either (Either(..), either)
import Data.Maybe (Maybe(..), maybe)
import Dispatcher.React (ReactReaderT, getProps)
import Effect.Aff (Aff)
import Effect.Class (class MonadEffect, liftEffect)
import Effect.Uncurried (EffectFn1, runEffectFn1)
import Network.HTTP.Affjax (AffjaxResponse)
import Network.HTTP.Affjax as Ajax
import Network.HTTP.Affjax.Request as Req
import Network.HTTP.Affjax.Response as Resp
import Network.HTTP.StatusCode (StatusCode(..))
import OEQ.Data.Error (ErrorResponse, decodeError, mkUniqueError)
import OEQ.Environment (baseUrl)

getJson :: forall a. String -> (Json -> Either String a) -> Aff (Either ErrorResponse a)
getJson path decode = do 
    response <- Ajax.get Resp.json (baseUrl <> path)
    pure $ decodeResponse decode response

postJson :: String -> Json -> Aff (Either ErrorResponse (AffjaxResponse String))
postJson = postJsonExpect 200

postJson' :: (AffjaxResponse String -> Maybe ErrorResponse) -> String -> Json -> Aff (Either ErrorResponse (AffjaxResponse String))
postJson' f path j = do 
    resp <- (Ajax.post (Resp.string) (baseUrl <> path) $ Req.json j)
    pure $ maybe (pure resp) Left $ f resp

postJsonExpect :: Int -> String -> Json -> Aff (Either ErrorResponse (AffjaxResponse String))
postJsonExpect status = postJson' (statusMatch (StatusCode status) maybeError)

statusMatch :: forall a. StatusCode -> (a -> Maybe ErrorResponse) -> AffjaxResponse a -> Maybe ErrorResponse
statusMatch code _ {status} | status == code = Nothing
statusMatch code f {status,response} = f response <|> (Just $ wrongCode status code)

wrongCode :: StatusCode -> StatusCode -> ErrorResponse 
wrongCode (StatusCode actual) (StatusCode expected) = mkUniqueError actual ("Expected status " <> show expected) Nothing

maybeError :: String -> Maybe ErrorResponse
maybeError = either (const Nothing) Just <<< (jsonParser >=> decodeError)

decodeResponse :: forall a. (Json -> Either String a) -> AffjaxResponse Json -> Either ErrorResponse a
decodeResponse decode resp = case resp.status of 
    (StatusCode 200) -> lmap (\d -> mkUniqueError 500 "Invalid JSON response" $ Just d) 
            $ decode resp.response
    (StatusCode code) -> Left $ case decodeError resp.response of 
        Left _ -> mkUniqueError code (titleForCode code) Nothing
        Right err -> err

titleForCode :: Int -> String
titleForCode 404 = "Page not found"
titleForCode _ = "Server error"

errorOr :: forall r m s a. Monad m => MonadEffect m => (a -> ReactReaderT {onError :: EffectFn1 ErrorResponse Unit |r} s m Unit) -> Either ErrorResponse a -> ReactReaderT {onError :: EffectFn1 ErrorResponse Unit |r} s m Unit
errorOr f = either (\e -> do 
    {onError} <- getProps
    liftEffect $ runEffectFn1 onError e) f 

