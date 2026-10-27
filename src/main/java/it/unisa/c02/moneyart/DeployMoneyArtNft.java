package it.unisa.c02.moneyart;

import it.unisa.c02.moneyart.model.blockchain.MoneyArtNft;
import java.math.BigInteger;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

public class DeployMoneyArtNft {
    public static void main(String[] args) throws Exception {
        String privateKey = System.getenv("GANACHE_PRIVATE_KEY");
        if (privateKey == null || privateKey.trim().isEmpty()) {
            throw new IllegalStateException("Imposta la variabile GANACHE_PRIVATE_KEY");
        }

        Web3j web3j = Web3j.build(new HttpService("http://127.0.0.1:7545"));
        try {
            Credentials credentials = Credentials.create(privateKey);
            MoneyArtNft contract = MoneyArtNft.deploy(
                            web3j,
                            credentials,
                            BigInteger.valueOf(20_000_000_000L),
                            BigInteger.valueOf(6_721_975L))
                    .send();

            System.out.println("Contratto deployato a: " + contract.getContractAddress());
        } finally {
            web3j.shutdown();
        }
    }
}