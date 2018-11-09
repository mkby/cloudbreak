az ad app create \
	--display-name cloudbreak-app \
	--password 'Cloudbreak123!' \
	--homepage ${cloudbreakAddress} \
	--identifier-uris ${identifierURI} \
	--reply-urls ${cloudbreakReplyUrl} \
	--end-date '${expirationDate}' \
	--required-resource-accesses '[{"resourceAppId":"797f4846-ba00-4fd7-ba43-dac1f8f63013","resourceAccess":[{"id":"41094075-9dad-400e-a0bd-54e686782033","type":"Scope"}]}]'
