# UniversalToken
Token Issue and Transfer based on Corda

start UniversalTokenIssueFlow owner: CryptoBankLondon, amount: 10000
run vaultQuery contractStateType: com.universaltoken.states.UniversalToken
start UniversalTokenTransferInitiatorFlow issuer: CryptoNotary, amount: 10000, receiver: CryptoBankSingapore
