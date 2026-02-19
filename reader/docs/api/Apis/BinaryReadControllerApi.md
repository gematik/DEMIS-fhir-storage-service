# BinaryReadControllerApi

All URIs are relative to *http://localhost*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**findById1**](BinaryReadControllerApi.md#findById1) | **GET** /Binary/{id} |  |
| [**search1**](BinaryReadControllerApi.md#search1) | **GET** /Binary |  |


<a name="findById1"></a>
# **findById1**
> Object findById1(id)



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

<a name="search1"></a>
# **search1**
> Object search1(requestParams)



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

