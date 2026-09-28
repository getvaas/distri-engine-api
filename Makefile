.PHONY: all

LOCATION=deploy/terraform

ENV?=dev

help:
	@echo "Execute: make <plan|apply> ENV=<(dev)|stg|prod>"

plan:
	cd $(LOCATION) && \
	rm -rf .terraform* && \
	terraform init -backend-config=configuration/$(ENV)/backend.conf \
	  -backend-config=configuration/$(ENV)/profile.tfvars && \
	terraform plan \
	  -var-file=configuration/$(ENV)/vars.tfvars \
	  -var-file=configuration/global.tfvars \
	  -var-file=configuration/$(ENV)/profile.tfvars

apply:
	cd $(LOCATION) && \
	rm -rf .terraform* && \
	terraform init -backend-config=configuration/$(ENV)/backend.conf \
	  -backend-config=configuration/$(ENV)/profile.tfvars && \
	terraform apply \
	  -var-file=configuration/$(ENV)/vars.tfvars \
	  -var-file=configuration/global.tfvars \
	  -var-file=configuration/$(ENV)/profile.tfvars

clean:
	cd $(LOCATION) && \
	rm -rf .terraform*

just-plan:
	cd $(LOCATION) && \
	terraform plan \
	  -var-file=configuration/$(ENV)/vars.tfvars \
	  -var-file=configuration/global.tfvars \
	  -var-file=configuration/$(ENV)/profile.tfvars
