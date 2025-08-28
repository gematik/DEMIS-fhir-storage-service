# BundleReadControllerApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**findById**](BundleReadControllerApi.md#findById) | **GET** /notification-clearing-api/fhir/Bundle/{id} |  |
| [**search**](BundleReadControllerApi.md#search) | **GET** /notification-clearing-api/fhir/Bundle |  |


<a name="findById"></a>
# **findById**
> Object findById(id)



### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **id** | **UUID**|  | [default to null] |

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

<a name="search"></a>
# **search**
> Object search(requestParams)



### Parameters

|Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **requestParams** | [**MultiValueMapStringString**](../Models/List.md)|  | [default to null] |

### Return type

**Object**

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: */*

