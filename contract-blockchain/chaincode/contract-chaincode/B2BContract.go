package main

import (
	"encoding/json"
	"fmt"
	"time"

	"github.com/hyperledger/fabric/core/chaincode/shim"
	"github.com/hyperledger/fabric/protos/peer"
)

// Define the Smart Contract structure
type SmartContract struct {
}

// Define Calculation sheet Items
type CalculationSheetItems struct{
	ItemUUID string `json:"ItemUUID"`
	ItemID string `json:"ItemID"`
	ChargeType string `json:"ChargeType"`
	InstructionCode	string `json:"InstructionCode"`
	OperationCode	string `json:"OperationCode"`
	ParentItemUUID string `json:"ParentItemUUID"`
	RateAmount float32 `json:"RateAmount"`
	RateCurrency string `json:"RateCurrency"`
	MinAmount	float32 `json:"MinAmount"`
	MinCurrency	string `json:"MinCurrency"`
	MaxAmount	float32 `json:"MaxAmount"`
	MaxCurrency	string `json:"MaxCurrency"`
	CalculationBase string `json:"CalculationBase"`
	PriceUnit	string `json:"PriceUnit"`
	PriceUnitUoM	string `json:"PriceUnitUoM"`
	ResolutionBase string `json:"ResolutionBase"`
	CalculationMethod	string `json:"CalculationMethod"`
}

// Define agreement items
type B2BContractItems struct{
	FreightContractId string `json:"FreightContractId"`
	FreightContractUUID	string `json:"FreightContractUUID"`
	ItemUUID string `json:"ItemUUID"`
	ItemId string `json:"ItemId"`
	StageType	string `json:"StageType"`
	ShippingType string `json:"ShippingType"`
	Items []CalculationSheetItems `json:"CalcSheetItems"`
}

// Define the SAP TM B2B Contract structure
type B2BContract struct{
	FreightContractUUID	string `json:"FreightContractUUID"`
	FreightContractID	string `json:"FreightContractID"`
	ExternalFreightContractID	string `json:"ExternalFreightContractID"`
	ContractDescription	string `json:"ContractDescription"`
	ValidityStart	time.Time `json:"ValidityStart"`
	ValidityEnd	time.Time `json:"ValidityEnd"`
	BP1Id	string `json:"BP1Id"`
	BP1Role	string `json:"BP1Role"`
	BP1Desc	string `json:"BP1Desc"`
	BP2Id	string `json:"BP2Id"`
	BP2Role string `json:"BP2Role"`
	BP2Desc	string `json:"BP2Desc"`
	Currency string `json:"Currency"`
	ShippingType string `json:"ShippingType"`
	ModeOfTransport	string `json:"ModeOfTransport"`
	CalculationSheet []B2BContractItems `json:"CalculationSheet"`
}

/*
 * The Init method is called when the Smart Contract "B2BContract" is instantiated by the blockchain network
 * Best practice is to have any Ledger initialization in separate function -- see initLedger()
 */
func (s *SmartContract) Init(APIstub shim.ChaincodeStubInterface) peer.Response {
	return shim.Success(nil)
}

/*
 * The Invoke method is called as a result of an application request to run the Smart Contract B2BContract
 * The calling application program should specify the particular smart contract function to be called, with arguments
 */
func (s *SmartContract) Invoke(APIstub shim.ChaincodeStubInterface) peer.Response {

	// Retrieve the requested Smart Contract function and arguments
	function, args := APIstub.GetFunctionAndParameters()
	var result string
	var err error
	fmt.Println(function)
	fmt.Println(args)
	// Route to the appropriate handler function to interact with the ledger appropriately
	if function == "queryContract" {
		result, err = queryContract(APIstub, args)
	} else if function == "updateContract" {
		result, err = updateContract(APIstub, args)
	} else if function == "createContract" {
		result, err = createContract(APIstub, args)
	}
	if err != nil {
		return shim.Error(err.Error())
	}
	// Return the result as success payload
	return shim.Success([]byte(result))
}

func createContract(APIstub shim.ChaincodeStubInterface, args []string) (string, error) {
	if len(args) != 1 {
		fmt.Println("Incorrect number of arguments. Expecting 1")
		return "", fmt.Errorf("Incorrect number of arguments. Expecting 1")
	}
	var contract []B2BContract
	json.Unmarshal([]byte(args[0]), &contract)
	result, err := APIstub.GetState(contract[0].ExternalFreightContractID)
	if err != nil{
		return "", fmt.Errorf("Failed to get state of the ledger. Try after some time")
	}
	if result != nil{
		return "", fmt.Errorf("Contract %s already exists", contract[0].ExternalFreightContractID)
	}
	contractValue, _ := json.Marshal(contract)
	createError := APIstub.PutState(contract[0].ExternalFreightContractID, contractValue)
	if createError != nil {
		fmt.Println("Failed to create contract: %s", contract[0].ExternalFreightContractID)
		return "", fmt.Errorf("Failed to create contract: %s", contract[0].ExternalFreightContractID)
	}
	fmt.Println(contract[0].ExternalFreightContractID, "saved successfully")

	return contract[0].ExternalFreightContractID + "saved", nil
}

func queryContract(APIstub shim.ChaincodeStubInterface, args []string) (string, error) {

	if len(args) != 1 {
		fmt.Println("Incorrect arguments. Enter the enternal freight agreement ID")
		return "", fmt.Errorf("Incorrect arguments. Enter the enternal freight agreement ID")
	}

	contractValue, err := APIstub.GetState(args[0])
	if err != nil {
		fmt.Println("Failed to get contract: %s", args[0])
		return "", fmt.Errorf("Failed to get contract: %s. Error: %s", args[0], err)
	}
	if contractValue == nil {
		fmt.Println("Failed to get contract: %s", args[0])
		return "", fmt.Errorf("Contract not found: %s", args[0])
	}
	return string(contractValue), nil
}

func updateContract(APIstub shim.ChaincodeStubInterface, args []string) (string, error) {

	if len(args) != 1 {
		fmt.Println("Incorrect number of arguments. Expecting 1")
		return "", fmt.Errorf("Incorrect number of arguments. Expecting 1")
	}
	var contract []B2BContract
	json.Unmarshal([]byte(args[0]), &contract)
	contractValue, err := APIstub.GetState(contract[0].ExternalFreightContractID)
	if err != nil {
		fmt.Println("Failed to get contract: %s", contract[0].ExternalFreightContractID)
		return "", fmt.Errorf("Failed to get contract: %s. Error: %s", contract[0].ExternalFreightContractID, err)
	}
	if contractValue == nil {
		fmt.Println("Failed to get contract: %s", contract[0].ExternalFreightContractID)
		return "", fmt.Errorf("Contract not found: %s", contract[0].ExternalFreightContractID)
	}
	var contractOld B2BContract

	json.Unmarshal(contractValue, &contractOld)
	contractOld.ContractDescription = contract[0].ContractDescription
	contractOld.ValidityStart = contract[0].ValidityStart
	contractOld.ValidityEnd = contract[0].ValidityEnd
	contractOld.BP1Id = contract[0].BP1Id
	contractOld.BP1Role = contract[0].BP1Role
	contractOld.BP1Desc = contract[0].BP1Desc
	contractOld.BP2Id = contract[0].BP2Id
	contractOld.BP2Role = contract[0].BP2Role
	contractOld.BP2Desc = contract[0].BP2Desc
	contractOld.Currency = contract[0].Currency
	contractOld.ShippingType = contract[0].ShippingType
	contractOld.ModeOfTransport = contract[0].ModeOfTransport
	contractOld.CalculationSheet = contract[0].CalculationSheet
	contractValueUpd, _ := json.Marshal(contract)
	err = APIstub.PutState(contract[0].ExternalFreightContractID, contractValueUpd)

	if err != nil {
		fmt.Println("Update of contract %s failed", args[2])
		return "", fmt.Errorf("Update of contract %s failed", args[2])
	}
	return args[2], nil
}

// main function starts up the chaincode in the container during instantiate
func main() {
    if err := shim.Start(new(SmartContract)); err != nil {
            fmt.Printf("Error starting B2BContract chaincode: %s", err)
    }
}