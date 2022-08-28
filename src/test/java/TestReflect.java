import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.vk.api.sdk.client.AbstractQueryBuilder;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.ServiceActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.queries.groups.GroupsGetByIdQueryWithLegacy;

import java.lang.reflect.*;

public class TestReflect {
	public static void main(String[] args) throws ClientException, ApiException {
		new Gson();

		VkApiClient vk = new VkApiClient(HttpTransportClient.getInstance());
		GroupsGetByIdQueryWithLegacy req = vk.groups().getByIdLegacy(new ServiceActor(0, "")).groupId("");
//		call(req, new TypeToken<List<GetByIdLegacyResponse>>() {}.getType());


//		GroupsGetByIdQueryWithLegacy.class.getGenericSuperclass().getClass().getTypeParameters()
//		Type type = new TypeToken<List<GetByIdLegacyResponse>>() {}.getType();
//		vkApi.call(req, new LinkedList<GetByIdLegacyResponse>())
	}

	private static <T> T foo(Type typeOfT) {
		TypeToken<T> typeToken = (TypeToken<T>) TypeToken.get(typeOfT);
		TypeAdapter<T> typeAdapter = new Gson().getAdapter(typeToken);
//		T object = typeAdapter.read(reader);
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T> T call(AbstractQueryBuilder<?,?> request) throws ClientException, ApiException {
		TypeToken<T> typeToken = (TypeToken<T>) TypeToken.get(extractType(request));
		TypeAdapter<T> typeAdapter = new Gson().getAdapter(typeToken);
//		T object = typeAdapter.read(reader);
//		return type2Class(type).cast(request.execute());
		return null;
	}

	private static Type extractType(AbstractQueryBuilder<?,?> object) {
		ParameterizedType genericType = (ParameterizedType) object.getClass().getGenericSuperclass();
		return genericType.getActualTypeArguments()[1];
	}

	public static Class<?> type2Class(Type type) {
		if (type instanceof Class) {
			return (Class<?>) type;
		} else if (type instanceof GenericArrayType) {
			return Array.newInstance(type2Class(((GenericArrayType)type).getGenericComponentType()), 0).getClass();
		} else if (type instanceof ParameterizedType) {
			return type2Class(((ParameterizedType) type).getRawType());
		} else if (type instanceof TypeVariable) {
			Type[] bounds = ((TypeVariable<?>) type).getBounds();
			return bounds.length == 0 ? Object.class : type2Class(bounds[0]);
		} else if (type instanceof WildcardType) {
			Type[] bounds = ((WildcardType) type).getUpperBounds();
			return bounds.length == 0 ? Object.class : type2Class(bounds[0]);
		} else {
			throw new UnsupportedOperationException("cannot handle type class: " + type.getClass());
		}
	}
}
