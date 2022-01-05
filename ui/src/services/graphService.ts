import { v2, rest } from './axiosInstances'
import { QueryParameters, GraphNodesApiResponse, ResourceDefinitionsApiResponse, PreFabGraph } from '@/types'
import { queryParametersHandler } from './serviceHelpers'

const endpoint = '/graphs/nodes/nodes'

const getGraphNodesNodes = async (queryParameters?: QueryParameters): Promise<GraphNodesApiResponse | false> => {
  let endpointWithQueryString = ''

  if (queryParameters) {
    endpointWithQueryString = queryParametersHandler(queryParameters, endpoint)
  }

  try {
    const resp = await v2.get(endpointWithQueryString || endpoint)

    // no content from server
    if (resp.status === 204) {
      return { vertices: [], edges: [] }
    }

    return resp.data
  } catch (err) {
    return false
  }
}

const getGraphDefinitionsByResourceId = async (id: string): Promise<ResourceDefinitionsApiResponse> => {
  try {
    const resp = await rest.get(`/graphs/for/${id}`)
    return resp.data
  } catch (err) {
    return (<unknown>{ name: [] }) as ResourceDefinitionsApiResponse
  }
}

const getDefinitionData = async (definition: string): Promise<PreFabGraph | null> => {
  try {
    const resp = await rest.get(`/graphs/${definition}`)
    return resp.data
  } catch (err) {
    return null
  }
}

export { getGraphNodesNodes, getGraphDefinitionsByResourceId, getDefinitionData }
