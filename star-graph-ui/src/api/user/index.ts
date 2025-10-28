import request from "@/utils/request";

class UserAPI {
  /**
   * 登录API
   *
   * @param data {LoginData}
   * @returns
   */
  static login(data) {
    const formData = new FormData();
    formData.append("username", data.username);
    formData.append("password", data.password);
    return request<any>({
      url: "/api/1.0/user/login",
      method: "post",
      data: formData
    });
  }
}

export default UserAPI;
