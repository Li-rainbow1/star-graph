import request from "@/utils/request";

class Text2ImageAPI {

    /**
     * 添加生成任务
     *
     * @param data {LoginData}
     * @returns
     */
    static propmt(data) {
        return request<any>({
            url: "/api/authed/1.0/t2i/propmt",
            method: "post",
            data: data
        });
    }

    /**
     * 取消任务
     *
     * @param data {LoginData}
     * @returns
     */
    static canelGen(data) {
        return request<any>({
            url: "/api/authed/1.0/t2i/canel",
            method: "post",
            data: data
        });
    }

    /**
     * 优先任务
     *
     * @param data {LoginData}
     * @returns
     */
    static proprityTask(data) {
        return request<any>({
            url: "/api/authed/1.0/t2i/proprity",
            method: "post",
            data: data
        });
    }

    /**
     * 加载图片列表
     *
     * @param data {LoginData}
     * @returns
     */
    static listImages(data) {
        return request<any>({
            url: "/api/authed/1.0/t2i/list",
            method: "post",
            data: data
        });
    }

    /**
     * 获取任务实时排名
     *
     * @param data {tempId: string}
     * @returns Promise<number | null> 当前排队序号，null表示任务已完成或被取消
     */
    static getTaskRank(data) {
        return request<any>({
            url: "/api/authed/1.0/t2i/rank",
            method: "post",
            data: data
        });
    }
}

export default Text2ImageAPI;
